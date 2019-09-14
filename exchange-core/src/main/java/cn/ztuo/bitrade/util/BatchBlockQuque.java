package cn.ztuo.bitrade.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class BatchBlockQuque<T>{
    private LinkedBlockingQueue<T> queue = new LinkedBlockingQueue<>(100);
    public List<T> getMessage(){
        List<T> s = new ArrayList<>();
        try {
            int size = queue.size();
            if (size > 1){
                size = size > 100 ? 100 : size;
                for (int i = 0; i < size; i++) {
                    T message = queue.take();
                    s.add(message);
                }
            } else {
                T message = queue.take();
                s.add(message);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return s;
    }
    public void putMessage(T t){
        try {
            queue.put(t);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
