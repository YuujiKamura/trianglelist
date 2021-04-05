# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

cmake_minimum_required(VERSION 3.4.1)


include_directories(
        atl71/
)

include_directories(
        mfc42/
)

add_library(
        sample-main
        SHARED
        main.cpp)
