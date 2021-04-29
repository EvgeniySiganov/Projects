package com.orderlock;

/*
The class determines whether there is a order the locks in method someMethodWithSynchronizedBlocks
 */
public class OrderLock {
    //when false the main thread spinning in the loop while
    static volatile boolean isInnerThreadBlocked = false;
    //when false the order lock is reverse (obj2, obj1)
    static volatile boolean flag = false;

    /*
    method in which determines order lock
     */
    public void someMethodWithSynchronizedBlocks(Object obj1, Object obj2) {
        //first lock if normal obj1
        synchronized (obj2) {
            //second
            synchronized (obj1) {
                System.out.println(obj1 + " " + obj2);
            }
        }
    }

    /*
    returns true if order locks in method someMethodWithSynchronizedBlocks is normal (obj1, obj2)
     */
    public static boolean isLockOrderNormal(final OrderLock orderLock, final Object o1, final Object o2) throws Exception {
        //from the beginning test and until the end the lock o1 will captured
        synchronized (o1) {
            Thread threadOuter = new Thread(new Runnable() {
                @Override
                public void run() {
                    Thread threadInner = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //start test until lock o1 is captured
                            orderLock.someMethodWithSynchronizedBlocks(o1, o2);
                        }
                    });
                    threadInner.start();
                    //spinning until threadInner is blocked
                    while (threadInner.getState() != Thread.State.BLOCKED) {
                        //let's get out of loop the main thread
                        isInnerThreadBlocked = true;
                    }
                    //if order normal this threadOuter take the lock o2, if don't then threadInner already got lock o2
                    // and this threadOuter will be wait before synchronized block
                    synchronized (o2) {
                        flag = true;
                    }
                }
            });
            threadOuter.setDaemon(true);
            threadOuter.start();

            //wait until the threadInner will be blocked
            while (!isInnerThreadBlocked) {
                Thread.sleep(1);
            }
            //wait until by normal order threadOuter quickly terminated
            //or by revers order threadOuter will be blocked by attempt take the lock o2
            while (threadOuter.isAlive() && threadOuter.getState() != Thread.State.BLOCKED) {
                Thread.sleep(1);
            }
        }

        return flag;
    }


    public static void main(String[] args) throws Exception {
        final OrderLock orderLock = new OrderLock();
        final Object o1 = new Object();
        final Object o2 = new Object();

        System.out.println(isLockOrderNormal(orderLock, o1, o2));
    }
}
