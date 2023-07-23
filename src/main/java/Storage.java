

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Storage implements Serializable {

    private ConcurrentHashMap<String, List<String>> topics; // <topico1, mensagens[msg1, msg2,...]>
    private ConcurrentHashMap<String, Integer> mapping; // <subId//topic, indexMessage>
    private int msg_id;

    public Storage(){
        this.topics = new ConcurrentHashMap<>();
        this.mapping = new ConcurrentHashMap<>();
        this.msg_id = 0;
    }

    public void setMsg_id(int msg_id) {
        this.msg_id = msg_id;
    }

    public int getMsg_id() {
        return msg_id;
    }

    public ConcurrentHashMap<String, Integer> getMapping() {
        return mapping;
    }

    public ConcurrentHashMap<String, List<String>> getTopics() {
        return topics;
    }

    public void setMapping(ConcurrentHashMap<String, Integer> mapping) {
        this.mapping = mapping;
    }

    public void setTopics(ConcurrentHashMap<String, List<String>> topics) {
        this.topics = topics;
    }
}
