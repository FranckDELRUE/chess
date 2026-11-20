cmake_minimum_required(VERSION 3.22.1)
project(stockfish_jni CXX)

set(SF ${CMAKE_CURRENT_SOURCE_DIR}/stockfish)

set(SF_SOURCES
    ${SF}/attacks.cpp ${SF}/benchmark.cpp ${SF}/bitboard.cpp ${SF}/evaluate.cpp ${SF}/main.cpp
    ${SF}/misc.cpp ${SF}/movegen.cpp ${SF}/movepick.cpp ${SF}/position.cpp
    ${SF}/search.cpp ${SF}/thread.cpp ${SF}/timeman.cpp ${SF}/tt.cpp ${SF}/uci.cpp
    ${SF}/ucioption.cpp ${SF}/tune.cpp ${SF}/syzygy/tbprobe.cpp
    ${SF}/nnue/nnue_accumulator.cpp ${SF}/nnue/nnue_misc.cpp ${SF}/nnue/network.cpp
    ${SF}/nnue/features/half_ka_v2_hm.cpp ${SF}/nnue/features/full_threats.cpp
    ${SF}/nnue/features/pp_3wide.cpp
    ${SF}/engine.cpp ${SF}/score.cpp ${SF}/memory.cpp)

# Stockfish's own entry point becomes a callable function (see stockfish_jni.cpp).
set_source_files_properties(${SF}/main.cpp PROPERTIES COMPILE_DEFINITIONS "main=stockfish_main")

add_library(stockfish_jni SHARED ${SF_SOURCES} stockfish_jni.cpp)

# Flags mirror the official Makefile for ARCH=armv8-dotprod (Cortex-A78 supports dot product).
# The network is loaded from a file (EvalFile), not embedded in the binary.
target_compile_definitions(stockfish_jni PRIVATE
    NDEBUG IS_64BIT USE_POPCNT USE_NEON=8 USE_NEON_DOTPROD NNUE_EMBEDDING_OFF)
target_compile_options(stockfish_jni PRIVATE
    -O3 -funroll-loops -fno-exceptions -march=armv8.2-a+dotprod)
target_link_libraries(stockfish_jni PRIVATE log)
