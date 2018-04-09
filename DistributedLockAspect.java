

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author cydyhty
 * @create 2018-03-09 11:19
 **/

@Aspect
@Configuration
@ConditionalOnClass(Config.class)
public class DistributedLockAspect {

    @Autowired
    private RedissonDistributedLocker redissonDistributedLocker;

    @Pointcut("@annotation(com.zqkh.common.configuration.lock.DistributedLock)")
    public void DistributedLockAspect() {
    }

    @Around(value = "DistributedLockAspect()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        Class targetClass = pjp.getTarget().getClass();
        String methodName = pjp.getSignature().getName();
        Class[] parameterTypes = ((MethodSignature) pjp.getSignature()).getMethod().getParameterTypes();
        Method method = targetClass.getMethod(methodName, parameterTypes);
        final String lockName = getLockName(targetClass,method);
        return lock(pjp, method, lockName);
    }



    @AfterThrowing(value = "DistributedLockAspect()", throwing = "ex")
    public void afterThrowing(Throwable ex) {
        throw new RuntimeException(ex);
    }

    String getLockName(Class clazz,Method method) {
        Objects.requireNonNull(method);
        DistributedLock annotation = method.getAnnotation(DistributedLock.class);
        String lockName = annotation.lockName();
        if(StringUtils.isEmpty(lockName)){
            StringBuffer sb = new StringBuffer();
            lockName = sb.append(clazz.getName()).append(method.getName()).toString();
        }
        return lockName;
    }


    Object lock(ProceedingJoinPoint pjp, Method method, final String lockName) {
        DistributedLock annotation = method.getAnnotation(DistributedLock.class);
        boolean tryLock = annotation.tryLock();
        if (tryLock) {
            return tryLock(pjp, annotation, lockName);
        } else {
            return lock(pjp, lockName);
        }
    }

    Object tryLock(ProceedingJoinPoint pjp, DistributedLock annotation, final String lockName) {
        int waitTime = annotation.waitTime(),
                leaseTime = annotation.leaseTime();
        TimeUnit timeUnit = annotation.timeUnit();
        RLock lock = redissonDistributedLocker.lock(lockName);
        try {
            boolean tryLock = lock.tryLock(waitTime,leaseTime,timeUnit);
            if(tryLock){
                return proceed(pjp);
            }else{
                throw new UnableToAquireLockException("获取锁资源失败");
            }
        } catch (Exception e){
            throw new UnableToAquireLockException("获取锁资源失败");
        }finally {
            redissonDistributedLocker.unlock(lockName);
        }
    }

    Object lock(ProceedingJoinPoint pjp, final String lockName) {
        RLock lock = redissonDistributedLocker.lock(lockName);
        try {
            lock.lock();
            return proceed(pjp);
        } finally {
            redissonDistributedLocker.unlock(lockName);
        }
    }

    Object proceed(ProceedingJoinPoint pjp) {
        try {
            return pjp.proceed();
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
}
