
#include <jni.h>

#include <lua.h>

#include <stdint.h>

#include <ffi.h>

#ifndef ENGINE_H_
#define ENGINE_H_

typedef struct {
	JNIEnv* env;
	jmethodID id;
	ffi_closure* closure;
} engine_jfuncwrapper;

typedef struct {
	lua_State* state;
	uint8_t closed;
	engine_jfuncwrapper** wrappers;
	size_t wrappers_amt;
} engine_inst;

void engine_setfunc(engine_inst* inst, JNIEnv* env, char* name, size_t name_len, jobject jfunc);

#endif // ENGINE_H_
