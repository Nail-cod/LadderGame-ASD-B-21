package src;

import java.util.*;

public class GameGraph {
    private int V;
    private List<List<Integer>> adj;

    public GameGraph(int v) {
        V = v;
        adj = new ArrayList<>(v);
        for (int i = 0; i < v; i++) adj.add(new ArrayList<>());
    }

    public void addEdge(int src, int dest) {
        if (src >= 0 && src < V && dest >= 0 && dest < V) {
            adj.get(src).add(dest);
        }
    }

    public List<Integer> getShortestPath(int start, int finish) {
        int[] dist = new int[V];
        int[] parent = new int[V];
        Arrays.fill(dist, Integer.MAX_VALUE);
        Arrays.fill(parent, -1);

        dist[start] = 0;
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
        pq.offer(new int[]{start, 0});

        while (!pq.isEmpty()) {
            int[] current = pq.poll();
            int u = current[0];
            int d = current[1];

            if (d > dist[u]) continue;
            if (u == finish) break;

            for (int v : adj.get(u)) {
                if (dist[u] + 1 < dist[v]) {
                    dist[v] = dist[u] + 1;
                    parent[v] = u;
                    pq.offer(new int[]{v, dist[v]});
                }
            }
        }

        List<Integer> path = new ArrayList<>();
        int curr = finish;
        while (curr != -1) {
            path.add(0, curr);
            curr = parent[curr];
        }

        if (path.isEmpty() || path.get(0) != start) return new ArrayList<>();
        return path;
    }
}
