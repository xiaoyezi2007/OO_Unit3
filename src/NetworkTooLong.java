import java.util.ArrayList;
import java.util.HashSet;

public class NetworkTooLong {
    private Network network;

    public NetworkTooLong(Network network) {
        this.network = network;
    }

    public int bfs(int node, int key) {
        if (node == key) {
            return 0;
        }
        HashSet<Integer> visited = new HashSet<>();
        ArrayList<Integer> queue = new ArrayList<>();
        ArrayList<Integer> len = new ArrayList<>();
        queue.add(node);
        visited.add(node);
        len.add(0);
        int begin = 0;
        int end = 0;
        while (begin <= end) {
            Person person = (Person) network.getPerson(queue.get(begin));
            for (Integer v : person.getAcquaintance().keySet()) {
                if (v == key) {
                    return len.get(begin) + 1;
                }
                if (!visited.contains(v)) {
                    queue.add(v);
                    visited.add(v);
                    len.add(len.get(begin) + 1);
                    end++;
                }
            }
            begin++;
        }
        return -1;
    }
}
