#include <jni.h>
#include <string>
extern "C" JNIEXPORT jstring JNICALL
Java_com_metalchess_MainActivity_nativeEngineName(JNIEnv* env, jobject) {
    return env->NewStringUTF("METAL CHESS native core");
}
