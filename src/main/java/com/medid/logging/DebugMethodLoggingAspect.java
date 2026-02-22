package com.medid.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
public class DebugMethodLoggingAspect {

    @Around("execution(* com.medid.controller..*(..)) || execution(* com.medid.service..*(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!log.isDebugEnabled()) {
            return joinPoint.proceed();
        }

        String method = joinPoint.getSignature().toShortString();
        String args = Arrays.stream(joinPoint.getArgs())
                .map(arg -> arg == null ? "null" : arg.getClass().getSimpleName())
                .collect(Collectors.joining(", "));

        long start = System.currentTimeMillis();
        log.debug("Entering {} with args [{}]", method, args);
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            log.debug("Exiting {} in {} ms", method, duration);
            return result;
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - start;
            log.debug("Exception in {} after {} ms: {}", method, duration, ex.getMessage());
            throw ex;
        }
    }
}
