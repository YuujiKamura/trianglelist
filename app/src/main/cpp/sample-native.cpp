#ifndef __SAMPLE_NATIVE_H_
#define __SAMPLE_NATIVE_H_

#include <jni.h>
#include <android/log.h>
#include <string>
#include "sfc/sample_main/main.cpp"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jstring JNICALL
Java_com_jpaver_trianglelist_SfcWriter_hello(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

JNIEXPORT void JNICALL
Java_com_jpaver_trianglelist_SfcWriter_main() {
    main();
    return;
}


#ifdef __cplusplus
}
#endif

#endif // __SAMPLE_NATIVE_H_