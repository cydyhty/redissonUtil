package com.zqkh.common.configuration.lock;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * @author hty
 * @create 2018-03-09 11:17
 **/

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {
    /**
     * 锁的名称。
     * 如果lockName可以确定，直接设置该属性。
     */
    String lockName() default "";
    /**
     * 是否使用尝试锁。
     */
    boolean tryLock() default true;
    /**
     * 最长等待时间。
     * 该字段只有当tryLock()返回true才有效。
     */
    int waitTime() default 3000;
    /**
     * 锁超时时间。
     * 超时时间过后，锁自动释放。
     * 建议：
     *   尽量缩简需要加锁的逻辑。
     */
    int leaseTime() default 1000;
    /**
     * 时间单位。默认为秒。
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

}