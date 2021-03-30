package org.apache.dubbo.rpc.proxy.jdk9;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.proxy.AbstractProxyInvoker;
import org.apache.dubbo.rpc.proxy.jdk.JdkProxyFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author qingshi
 * @email 705029004@qq.com
 * @date 2021/03/28 3:02 下午
 */
public class Jdk9ProxyFactory extends JdkProxyFactory {
    @Override
    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) throws RpcException {
        return new AbstractProxyInvoker<T>(proxy, type, url) {

            private ConcurrentHashMap<Tuple2, MethodHandle> lookMethodCache = new ConcurrentHashMap<>(4);

            class Tuple2 {
                public String methodName;
                public Class<?>[] parameterTypes;

                public Tuple2(String methodName, Class<?>[] parameterTypes) {
                    this.methodName = methodName;
                    this.parameterTypes = parameterTypes;
                }

                @Override
                public boolean equals(Object o) {
                    if (this == o) return true;
                    if (o == null || getClass() != o.getClass()) return false;
                    Tuple2 tuple2 = (Tuple2) o;
                    return Objects.equals(methodName, tuple2.methodName) && Arrays.equals(parameterTypes, tuple2.parameterTypes);
                }

                @Override
                public int hashCode() {
                    int result = Objects.hash(methodName);
                    result = 31 * result + Arrays.hashCode(parameterTypes);
                    return result;
                }
            }

            @Override
            protected Object doInvoke(T proxy, String methodName,
                                      Class<?>[] parameterTypes,
                                      Object[] arguments) throws Throwable {

                Tuple2 key = new Tuple2(methodName, parameterTypes);
                MethodHandle methodHandle = lookMethodCache.get(key);
                if (null == methodHandle) {
                    lookMethodCache.putIfAbsent(key, MethodHandles.lookup().unreflect(type.getMethod(methodName, parameterTypes)));
                }
                return lookMethodCache.get(key).invoke(proxy,arguments);
            }
        };
    }
}
