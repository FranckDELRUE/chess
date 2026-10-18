// JNI bridge: runs Stockfish's UCI loop in-process and exposes it through two pipes.
// std::cin / std::cout are rebound to the pipe ends, so the app's own stdio is untouched.
// One engine session per process: Stockfish keeps global state, so run -> quit is final.
#include <jni.h>
#include <unistd.h>

#include <cerrno>
#include <iostream>
#include <streambuf>
#include <string>

extern int stockfish_main(int argc, char* argv[]);

namespace {

int toEngine[2] = {-1, -1};    // Kotlin writes [1], Stockfish reads [0]
int fromEngine[2] = {-1, -1};  // Stockfish writes [1], Kotlin reads [0]

class FdInBuf : public std::streambuf {
   public:
    explicit FdInBuf(int fd) : fd_(fd) {}

   protected:
    int_type underflow() override {
        ssize_t n;
        do {
            n = ::read(fd_, buf_, sizeof(buf_));
        } while (n < 0 && errno == EINTR);
        if (n <= 0) return traits_type::eof();
        setg(buf_, buf_, buf_ + n);
        return traits_type::to_int_type(buf_[0]);
    }

   private:
    int fd_;
    char buf_[4096];
};

class FdOutBuf : public std::streambuf {
   public:
    explicit FdOutBuf(int fd) : fd_(fd) {}

   protected:
    std::streamsize xsputn(const char* s, std::streamsize n) override {
        std::streamsize done = 0;
        while (done < n) {
            ssize_t w = ::write(fd_, s + done, static_cast<size_t>(n - done));
            if (w < 0 && errno == EINTR) continue;
            if (w <= 0) break;
            done += w;
        }
        return done;
    }
    int_type overflow(int_type c) override {
        if (c == traits_type::eof()) return traits_type::not_eof(c);
        char ch = static_cast<char>(c);
        return xsputn(&ch, 1) == 1 ? c : traits_type::eof();
    }

   private:
    int fd_;
};

}  // namespace

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_chesscompanion_core_engine_StockfishNative_nativeInit(JNIEnv*, jobject) {
    if (toEngine[0] != -1) return JNI_FALSE;  // already used in this process
    return (pipe(toEngine) == 0 && pipe(fromEngine) == 0) ? JNI_TRUE : JNI_FALSE;
}

// Blocks until the engine receives "quit". Call from a dedicated thread.
JNIEXPORT void JNICALL
Java_com_chesscompanion_core_engine_StockfishNative_nativeRun(JNIEnv*, jobject) {
    FdInBuf in(toEngine[0]);
    FdOutBuf out(fromEngine[1]);
    std::cin.rdbuf(&in);
    std::cout.rdbuf(&out);
    char arg0[] = "stockfish";
    char* argv[] = {arg0, nullptr};
    stockfish_main(1, argv);
    std::cout.flush();
    close(fromEngine[1]);  // reader sees EOF
}

JNIEXPORT void JNICALL
Java_com_chesscompanion_core_engine_StockfishNative_nativeSend(JNIEnv* env, jobject, jstring line) {
    const char* chars = env->GetStringUTFChars(line, nullptr);
    std::string msg(chars);
    env->ReleaseStringUTFChars(line, chars);
    msg.push_back('\n');
    size_t done = 0;
    while (done < msg.size()) {
        ssize_t w = ::write(toEngine[1], msg.data() + done, msg.size() - done);
        if (w < 0 && errno == EINTR) continue;
        if (w <= 0) return;
        done += static_cast<size_t>(w);
    }
}

// Blocks for one line of engine output; returns null at end of stream.
JNIEXPORT jstring JNICALL
Java_com_chesscompanion_core_engine_StockfishNative_nativeReadLine(JNIEnv* env, jobject) {
    std::string line;
    char c;
    for (;;) {
        ssize_t n = ::read(fromEngine[0], &c, 1);
        if (n < 0 && errno == EINTR) continue;
        if (n <= 0) {
            if (line.empty()) return nullptr;
            break;
        }
        if (c == '\n') break;
        if (c != '\r') line.push_back(c);
    }
    return env->NewStringUTF(line.c_str());
}

}  // extern "C"
