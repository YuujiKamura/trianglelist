# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

cmake_minimum_required(VERSION 3.4.1)

include_directories(
        src/main/cpp/sfc/sample_main/
)

include_directories(
        src/main/cpp/sfc/common_lib/
)

include_directories(
        src/main/cpp/sfc/AtlCom/
)

include_directories(
        src/main/cpp/sfc/WinDDK/7600.16385.1/inc/mfc42/
)

include_directories(
        src/main/cpp/sfc/WinDDK/7600.16385.1/inc/atl71/
)



add_library(
        sample-native
        SHARED
        sample-native.cpp)

