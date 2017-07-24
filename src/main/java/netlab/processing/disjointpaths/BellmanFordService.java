package netlab.processing.disjointpaths;

import lombok.extern.slf4j.Slf4j;
import netlab.topology.elements.Link;
import netlab.topology.elements.Node;
import netlab.topology.elements.Topology;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BellmanFordService {

    public List<Link> shortestPath(Topology topo, Node source, Node dest){

        Map<Node, Link> edgeMap = bellmanFord(topo, source);
        if(edgeMap.isEmpty()){
            return new ArrayList<>();
        }

        return buildPath(dest, source, edgeMap);
    }

    public Map<Node, List<Link>> allShortestPaths(Topology topo, Node source){

        Map<Node, Link> edgeMap = bellmanFord(topo, source);
        if(edgeMap.isEmpty()){
            return new HashMap<>();
        }

        Map<Node, List<Link>> allPaths = new HashMap<>();

        Set<Node> vertices = topo.getNodes();

        for(Node vertex : vertices){
            allPaths.put(vertex, buildPath(vertex, source, edgeMap));
        }

        return allPaths;
    }

    private List<Link> buildPath(Node vertex, Node source, Map<Node, Link> edgeMap){
        List<Link> path = new ArrayList<>();
        Node currentNode = vertex;
        while(!currentNode.equals(source)){
            if(!edgeMap.containsKey(currentNode)){
                path = new ArrayList<>();
                break;
            }
            Link edge = edgeMap.get(currentNode);
            path.add(edge);
            currentNode = edge.getOrigin();
        }
        Collections.reverse(path);
        return path;
    }

    private Map<Node, Link> bellmanFord(Topology topo, Node source){

        Map<Node, Long> distanceMap = new HashMap<>();
        Map<Node, Link> edgeMap = new HashMap<>();

        Set<Node> vertices = topo.getNodes();
        List<Link> edges = topo
                .getLinks()
                .stream()
                .sorted((a,z) -> a.getOrigin().getId().compareToIgnoreCase(z.getOrigin().getId()))
                .collect(Collectors.toList());

        for(Node vertex : vertices){
            distanceMap.put(vertex, 999999999L);
        }

        distanceMap.put(source, 0L);

        for(Integer i = 0; i < vertices.size()-1; i++){
            boolean noChanges = true;
            for(Link edge : edges){
                Node a = edge.getOrigin();
                Node z = edge.getTarget();

                if(!distanceMap.containsKey(a) || !distanceMap.containsKey(z))
                {
                    log.info("At least one vertex on an edge tgat does not exist in topology");
                    log.info("Edge: " + edge);
                    return new HashMap<>();
                }

                Long weight = distanceMap.get(a) + edge.getWeight();
                if(weight < distanceMap.get(z)){
                    distanceMap.put(z, weight);
                    edgeMap.put(z, edge);
                    noChanges = false;
                }
            }
            if(noChanges){
                break;
            }
        }

        return edgeMap;
    }
}
