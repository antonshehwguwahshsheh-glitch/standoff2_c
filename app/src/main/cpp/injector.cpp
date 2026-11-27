#include <jni.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <dlfcn.h>
#include <unistd.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "Injector"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT void JNICALL
Java_com_abusegpt_injector_MainActivity_injectNative(JNIEnv *env, jobject thiz, jint pid, jstring libPath) {
    const char *path = env->GetStringUTFChars(libPath, nullptr);
    if (ptrace(PTRACE_ATTACH, pid, nullptr, nullptr) == -1) {
        LOGI("Failed to attach");
        return;
    }
    waitpid(pid, nullptr, 0);
    void *handle = dlopen("libdl.so", RTLD_LAZY);
    if (handle) LOGI("Injecting %s", path);
    ptrace(PTRACE_DETACH, pid, nullptr, nullptr);
    env->ReleaseStringUTFChars(libPath, path);
}
