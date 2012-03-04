package com.github.mauricio.postgresql.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * User: Maurício Linhares
 * Date: 3/1/12
 * Time: 11:05 PM
 */
public class BasicFuture<T> implements Future<T> {

    private volatile boolean done;
    private volatile T result;
    private volatile Throwable e;

    @Override
    public boolean cancel(boolean b) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return this.done;
    }

    public void set(T result) {
        this.result = result;
        this.done = true;

    }

    @Override
    public T get() throws InterruptedException, ExecutionException {

        while ( !this.done ) {
            Thread.sleep(1000);
            System.out.println( "waiting" );
        }

        return this.getValue();
    }

    public void setError(Throwable e) {
        System.out.println( "Received error" );
        this.e = e;
        this.done = true;
    }

    @Override
    public T get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {

        long totalTime = timeUnit.toMillis(l);
        long increment = 500;
        long sum = 0;

        while (!this.done && sum <= totalTime) {
            Thread.sleep(increment);
            sum += increment;
        }

        if (this.done) {
            return this.getValue();
        } else {
            throw new TimeoutException("Lock reached the timeout limit");
        }
    }

    private T getValue() throws ExecutionException {
        if (this.e != null) {
            throw new ExecutionException(this.e);
        } else {
            return this.result;
        }
    }


}
