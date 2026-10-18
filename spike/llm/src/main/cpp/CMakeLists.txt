cmake_minimum_required(VERSION 3.22.1)
project(llm_jni C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# llama.cpp (MIT), pinned release; the archive is downloaded once per build directory.
include(FetchContent)
FetchContent_Declare(llama_cpp
    URL https://github.com/ggml-org/llama.cpp/archive/refs/tags/v0.7.0.tar.gz
    URL_HASH SHA256=55d67acf3ad2ad747a9bbeb75f9f0e173dbcc0d1753a471038a90c819421c5e1
    DOWNLOAD_EXTRACT_TIMESTAMP TRUE)

# Library only, statically linked into libllm_jni.so.
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_APP OFF CACHE BOOL "" FORCE)
set(LLAMA_OPENSSL OFF CACHE BOOL "" FORCE)
# CPU backend for the Cortex-A78 / A55 pair (ARMv8.2-A, dot product, fp16; no i8mm).
# ggml's own thread pool instead of OpenMP; KleidiAI left out (extra download, to try later).
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_SYSTEM_ARCH "ARM" CACHE STRING "" FORCE)
set(GGML_CPU_ARM_ARCH "armv8.2-a+dotprod+fp16" CACHE STRING "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_CPU_KLEIDIAI OFF CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(llama_cpp)

add_library(llm_jni SHARED llm_jni.cpp)
target_link_libraries(llm_jni PRIVATE llama log)
