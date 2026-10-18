// JNI bridge to llama.cpp for the S0-6 spike: load a GGUF model, generate one chat answer,
// stream the raw UTF-8 bytes of each token to Kotlin (decoding happens there).
// One handle = one model + one context; calls on a handle must not overlap (Kotlin serializes them).
#include <android/log.h>
#include <jni.h>

#include <atomic>
#include <chrono>
#include <string>
#include <vector>

#include "llama.h"

namespace {

constexpr const char* kTag = "LLM_JNI";

struct Session {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    llama_batch_ext* batch = nullptr;
    std::atomic<bool> cancel{false};
};

int64_t nowUs() {
    using namespace std::chrono;
    return duration_cast<microseconds>(steady_clock::now().time_since_epoch()).count();
}

// Only warnings and errors: llama.cpp's info output at load time would flood the Logcat buffer.
void logToAndroid(ggml_log_level level, const char* text, void*) {
    if (level == GGML_LOG_LEVEL_ERROR) __android_log_write(ANDROID_LOG_ERROR, kTag, text);
    if (level == GGML_LOG_LEVEL_WARN) __android_log_write(ANDROID_LOG_WARN, kTag, text);
}

void setTokens(llama_batch_ext* batch, const llama_token* tokens, int32_t n, llama_pos pos0) {
    llama_batch_ext_clear(batch);
    for (int32_t i = 0; i < n; ++i) {
        const int32_t idx = llama_batch_ext_add_token(batch, 0, tokens[i]);
        const llama_pos pos = pos0 + i;
        llama_batch_ext_set_pos(batch, idx, &pos);
    }
    llama_batch_ext_set_output_logits(batch, n - 1, true);
}

std::string jstr(JNIEnv* env, jstring s) {
    const char* chars = env->GetStringUTFChars(s, nullptr);
    std::string out(chars);
    env->ReleaseStringUTFChars(s, chars);
    return out;
}

// Applies the model's chat template (llama.cpp built-in detection) and appends the assistant prefix.
bool formatPrompt(const llama_model* model, const std::vector<std::string>& roles,
                  const std::vector<std::string>& contents, const std::string& prefix, std::string& out) {
    std::vector<llama_chat_message> chat;
    for (size_t i = 0; i < roles.size(); ++i) chat.push_back({roles[i].c_str(), contents[i].c_str()});
    const char* tmpl = llama_model_chat_template(model, nullptr);
    std::vector<char> buf(4096);
    int32_t n = llama_chat_apply_template(tmpl, chat.data(), chat.size(), true, buf.data(), buf.size());
    if (n > static_cast<int32_t>(buf.size())) {
        buf.resize(n);
        n = llama_chat_apply_template(tmpl, chat.data(), chat.size(), true, buf.data(), buf.size());
    }
    if (n < 0) return false;
    out.assign(buf.data(), n);
    out += prefix;
    return true;
}

}  // namespace

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_chesscompanion_spike_llm_LlamaNative_nativeSystemInfo(JNIEnv* env, jobject) {
    return env->NewStringUTF(llama_print_system_info());
}

// Returns a session handle, or 0 if the model or context cannot be created.
JNIEXPORT jlong JNICALL
Java_com_chesscompanion_spike_llm_LlamaNative_nativeLoad(JNIEnv* env, jobject, jstring path, jint threads,
                                                         jint nCtx) {
    llama_log_set(logToAndroid, nullptr);
    llama_backend_init();
    auto* s = new Session();
    llama_model_params mp = llama_model_default_params();
    mp.n_gpu_layers = 0;
    s->model = llama_model_load_from_file(jstr(env, path).c_str(), mp);
    if (s->model == nullptr) {
        delete s;
        return 0;
    }
    llama_context_params cp = llama_context_default_params();
    cp.n_ctx = static_cast<uint32_t>(nCtx);
    cp.n_batch = static_cast<uint32_t>(nCtx);
    cp.n_threads = threads;
    cp.n_threads_batch = threads;
    cp.no_perf = false;
    s->ctx = llama_init_from_model(s->model, cp);
    if (s->ctx == nullptr) {
        llama_model_free(s->model);
        delete s;
        return 0;
    }
    s->batch = llama_batch_ext_init(s->ctx);
    return reinterpret_cast<jlong>(s);
}

JNIEXPORT void JNICALL
Java_com_chesscompanion_spike_llm_LlamaNative_nativeSetThreads(JNIEnv*, jobject, jlong handle, jint threads) {
    llama_set_n_threads(reinterpret_cast<Session*>(handle)->ctx, threads, threads);
}

JNIEXPORT void JNICALL
Java_com_chesscompanion_spike_llm_LlamaNative_nativeCancel(JNIEnv*, jobject, jlong handle) {
    reinterpret_cast<Session*>(handle)->cancel = true;
}

// Generates one answer from an empty context. Each token's bytes go to callback.onToken(byte[]),
// which returns false to stop. Returns [promptTokens, prefillUs, generatedTokens, decodeSteps, decodeUs,
// firstTokenUs], or null on error. firstTokenUs runs from the start of prompt processing to the first
// sampled token; decodeSteps / decodeUs cover the single-token steps after it (sampling included).
JNIEXPORT jlongArray JNICALL
Java_com_chesscompanion_spike_llm_LlamaNative_nativeGenerate(JNIEnv* env, jobject, jlong handle,
                                                             jobjectArray roles, jobjectArray contents,
                                                             jstring assistantPrefix, jint maxTokens,
                                                             jfloat temperature, jint seed, jobject callback) {
    auto* s = reinterpret_cast<Session*>(handle);
    s->cancel = false;
    std::vector<std::string> r, c;
    const jsize n = env->GetArrayLength(roles);
    for (jsize i = 0; i < n; ++i) {
        r.push_back(jstr(env, static_cast<jstring>(env->GetObjectArrayElement(roles, i))));
        c.push_back(jstr(env, static_cast<jstring>(env->GetObjectArrayElement(contents, i))));
    }
    std::string prompt;
    if (!formatPrompt(s->model, r, c, jstr(env, assistantPrefix), prompt)) return nullptr;

    const llama_vocab* vocab = llama_model_get_vocab(s->model);
    const int32_t nPrompt = -llama_tokenize(vocab, prompt.c_str(), prompt.size(), nullptr, 0, true, true);
    std::vector<llama_token> tokens(nPrompt);
    if (llama_tokenize(vocab, prompt.c_str(), prompt.size(), tokens.data(), nPrompt, true, true) < 0) return nullptr;
    if (nPrompt + maxTokens > static_cast<int32_t>(llama_n_ctx(s->ctx))) return nullptr;

    llama_sampler* smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
    if (temperature <= 0.0f) {
        llama_sampler_chain_add(smpl, llama_sampler_init_greedy());
    } else {
        llama_sampler_chain_add(smpl, llama_sampler_init_top_k(40));
        llama_sampler_chain_add(smpl, llama_sampler_init_top_p(0.9f, 1));
        llama_sampler_chain_add(smpl, llama_sampler_init_temp(temperature));
        llama_sampler_chain_add(smpl, llama_sampler_init_dist(static_cast<uint32_t>(seed)));
    }

    jclass cbClass = env->GetObjectClass(callback);
    jmethodID onToken = env->GetMethodID(cbClass, "onToken", "([B)Z");

    llama_memory_clear(llama_get_memory(s->ctx), true);
    const int64_t t0 = nowUs();
    setTokens(s->batch, tokens.data(), nPrompt, 0);
    int64_t prefillUs = 0, firstTokenUs = 0;
    int32_t generated = 0, decodeSteps = 0;
    llama_pos pos = nPrompt;
    bool ok = true;
    for (int32_t step = 0; step < maxTokens && !s->cancel; ++step) {
        if (llama_process(s->ctx, LLAMA_PROCESS_TYPE_DECODE, s->batch) != 0) {
            ok = false;
            break;
        }
        if (step == 0) prefillUs = nowUs() - t0;
        else decodeSteps++;
        llama_token id = llama_sampler_sample(smpl, s->ctx, -1);
        if (step == 0) firstTokenUs = nowUs() - t0;
        if (llama_vocab_is_eog(vocab, id)) break;
        char piece[256];
        const int32_t len = llama_token_to_piece(vocab, id, piece, sizeof(piece), 0, false);
        generated++;
        if (len > 0) {
            jbyteArray bytes = env->NewByteArray(len);
            env->SetByteArrayRegion(bytes, 0, len, reinterpret_cast<const jbyte*>(piece));
            const jboolean more = env->CallBooleanMethod(callback, onToken, bytes);
            env->DeleteLocalRef(bytes);
            if (env->ExceptionCheck() || !more) break;
        }
        setTokens(s->batch, &id, 1, pos++);
    }
    const int64_t totalUs = nowUs() - t0;
    llama_sampler_free(smpl);
    if (!ok || env->ExceptionCheck()) return nullptr;

    const jlong stats[] = {nPrompt, prefillUs, generated, decodeSteps, totalUs - firstTokenUs, firstTokenUs};
    jlongArray out = env->NewLongArray(6);
    env->SetLongArrayRegion(out, 0, 6, stats);
    return out;
}

JNIEXPORT void JNICALL
Java_com_chesscompanion_spike_llm_LlamaNative_nativeFree(JNIEnv*, jobject, jlong handle) {
    auto* s = reinterpret_cast<Session*>(handle);
    llama_batch_ext_free(s->batch);
    llama_free(s->ctx);
    llama_model_free(s->model);
    delete s;
}

}  // extern "C"
