package netlab.topology.services;


import lombok.extern.slf4j.Slf4j;
import netlab.processing.shortestPaths.MinimumCostPathService;
import netlab.topology.elements.*;
import netlab.topology.elements.Node;
import netlab.topology.elements.Path;
import org.graphstream.graph.*;
import org.graphstream.graph.implementations.DefaultGraph;
import org.graphstream.stream.file.FileSource;
import org.graphstream.stream.file.FileSourceFactory;
import org.graphstream.stream.file.FileSourceGML;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TopologyService {

    private Map<String, Topology> topologyIdMap;

    private Map<String, Node> nodeIdMap;

    private Map<String, Link> linkIdMap;

    private MinimumCostPathService minimumCostPathService;

    @Autowired
    public TopologyService(MinimumCostPathService minimumCostPathService){
        this.minimumCostPathService = minimumCostPathService;
        topologyIdMap = new HashMap<>();
        topologyIdMap.put("nsfnet", makeNsfNet());
        topologyIdMap.put("tw", makeTWTelecom());

        nodeIdMap = new HashMap<>();
        linkIdMap = new HashMap<>();
        for(Topology topo : topologyIdMap.values()){
            for(Node node : topo.getNodes()){
                nodeIdMap.put(node.getId(), node);
            }
            for(Link link : topo.getLinks()){
                linkIdMap.put(link.getId(), link);
            }
        }
    }

    public Topology getTopologyById(String id){
        return topologyIdMap.getOrDefault(id.toLowerCase(), topologyIdMap.get("nsfnet"));
    }

    private Topology makeNsfNet() {
        Set<Node> nodes = new HashSet<>();
        Node seattle = new Node("Seattle", 47.6062, 122.3321);
        Node paloAlto = new Node("Palo Alto", 37.4419, 122.1430);
        Node sanDiego = new Node("San Diego", 32.7157, 117.1611);
        Node saltLakeCity = new Node("Salt Lake City", 40.7608, 111.8910);
        Node boulder = new Node("Boulder", 40.0150, 105.2705);
        Node houston = new Node("Houston", 29.7604, 95.3698);
        Node lincoln = new Node("Lincoln", 40.8258, 96.6852);
        Node champaign = new Node("Champaign", 40.1164, 88.2434);
        Node annArbor = new Node("Ann Arbor", 42.2808, 83.7430);
        Node pittsburgh = new Node("Pittsburgh", 40.4406, 79.9959);
        Node atlanta = new Node("Atlanta", 33.7490, 84.3880);
        Node collegePark = new Node("College Park", 38.9897, 76.9378);
        Node ithaca = new Node("Ithaca", 42.4440, 76.5019);
        Node princeton = new Node("Princeton", 40.3573, 74.6672);
        nodes.add(seattle);
        nodes.add(paloAlto);
        nodes.add(sanDiego);
        nodes.add(saltLakeCity);
        nodes.add(boulder);
        nodes.add(houston);
        nodes.add(lincoln);
        nodes.add(champaign);
        nodes.add(annArbor);
        nodes.add(pittsburgh);
        nodes.add(atlanta);
        nodes.add(collegePark);
        nodes.add(ithaca);
        nodes.add(princeton);
        Set<Link> links = new HashSet<>();
        links.add(new Link(seattle, paloAlto, 1100.0));
        links.add(new Link(seattle, sanDiego, 1600.0));
        links.add(new Link(seattle, champaign, 2800.0));
        links.add(new Link(paloAlto, seattle, 1100.0));
        links.add(new Link(paloAlto, sanDiego, 600.0));
        links.add(new Link(paloAlto, saltLakeCity, 1000.0));
        links.add(new Link(sanDiego, seattle, 1600.0));
        links.add(new Link(sanDiego, paloAlto, 600.0));
        links.add(new Link(sanDiego, houston, 2000.0));
        links.add(new Link(saltLakeCity, paloAlto, 1000.0));
        links.add(new Link(saltLakeCity, annArbor, 2400.0));
        links.add(new Link(saltLakeCity, boulder, 600.0));
        links.add(new Link(boulder, saltLakeCity, 600.0));
        links.add(new Link(boulder, houston, 1100.0));
        links.add(new Link(boulder, lincoln, 800.0));
        links.add(new Link(houston, sanDiego, 2000.0));
        links.add(new Link(houston, boulder, 1100.0));
        links.add(new Link(houston, collegePark, 2000.0));
        links.add(new Link(houston, atlanta, 1200.0));
        links.add(new Link(lincoln, boulder, 800.0));
        links.add(new Link(lincoln, champaign, 700.0));
        links.add(new Link(champaign, seattle, 2800.0));
        links.add(new Link(champaign, lincoln, 700.0));
        links.add(new Link(champaign, pittsburgh, 700.0));
        links.add(new Link(annArbor, saltLakeCity, 2400.0));
        links.add(new Link(annArbor, ithaca, 800.0));
        links.add(new Link(annArbor, princeton, 800.0));
        links.add(new Link(atlanta, houston, 1200.0));
        links.add(new Link(atlanta, pittsburgh, 900.0));
        links.add(new Link(pittsburgh, champaign, 700.0));
        links.add(new Link(pittsburgh, atlanta, 900.0));
        links.add(new Link(pittsburgh, princeton, 500.0));
        links.add(new Link(pittsburgh, ithaca, 500.0));
        links.add(new Link(collegePark, houston, 2000.0));
        links.add(new Link(collegePark, princeton, 300.0));
        links.add(new Link(collegePark, ithaca, 300.0));
        links.add(new Link(princeton, annArbor, 800.0));
        links.add(new Link(princeton, pittsburgh, 500.0));
        links.add(new Link(princeton, collegePark, 300.0));
        links.add(new Link(ithaca, annArbor, 800.0));
        links.add(new Link(ithaca, pittsburgh, 500.0));
        links.add(new Link(ithaca, collegePark, 300.0));
        Topology topo = new Topology("nsfnet", nodes, links);
        return populatePathCosts(topo);
    }

    public Topology makeTWTelecom(){
        String graphName = "tw";
        Graph g = readGraphModel(graphName);

        return makeTopologyFromGraph(g);
    }

    private Topology makeTopologyFromGraph(Graph g) {
        Set<Node> nodes = new HashSet<>();
        Map<String, Node> nodeIdMap = new HashMap<>();
        Map<String, Set<String>> nodeIdNeighborsMap = new HashMap<>();
        Set<Link> links = new HashSet<>();
        for(org.graphstream.graph.Node node : g.getNodeSet()){
            Iterator<org.graphstream.graph.Node> neighbors = node.getNeighborNodeIterator();
            // Skip nodes with no neighbors
            if(!neighbors.hasNext()){
                continue;
            }
            String label = node.getAttribute("ui.label");
            Double longitude = node.getAttribute("Longitude");
            Double latitude = node.getAttribute("Latitude");
            Location nodePoint = new Location(latitude, longitude);
            Node thisNode = new Node(label, nodePoint);
            nodes.add(thisNode);
            nodeIdMap.put(label, thisNode);
            nodeIdNeighborsMap.putIfAbsent(label, new HashSet<>());
            while(neighbors.hasNext()){
                org.graphstream.graph.Node neighbor = neighbors.next();
                String neighborLabel = neighbor.getAttribute("ui.label");
                nodeIdNeighborsMap.get(label).add(neighborLabel);
            }
        }

        for(String nodeId : nodeIdNeighborsMap.keySet()){
            Node origin = nodeIdMap.get(nodeId);
            for(String neighborId : nodeIdNeighborsMap.get(nodeId)){
                Node target = nodeIdMap.get(neighborId);
                Link otLink = new Link(origin, target, origin.getPoint().distanceTo(target.getPoint()));
                links.add(otLink);
            }
        }

        return new Topology(g.getId(), nodes, links);
    }


    public Graph readGraphModel(String graphName){
        String filePath = System.getProperty("user.dir") + "/config/topologies/" + graphName + ".gml";
        Graph g = new DefaultGraph(graphName);
        FileSource fs = new FileSourceGML();

        try {
            fs.addSink(g);
            fs.readAll(filePath);
        } catch( IOException e) {
            e.printStackTrace();
        } finally {
            fs.removeSink(g);
        }

        return g;
    }

    public Topology populatePathCosts(Topology topo) {
        Map<SourceDestPair, Path> allPairsPathMap = minimumCostPathService.findAllShortestPaths(topo);
        Map<SourceDestPair, Double> allPairsWeightMap = allPairsPathMap.keySet().stream()
                .collect(Collectors.toMap(p -> p, p -> allPairsPathMap.get(p).getTotalWeight()));
        topo.setMinimumPathCostMap(allPairsWeightMap);
        return topo;
    }

    public Node getNodeById(String id){
        return nodeIdMap.get(id);
    }

    public Link getLinkById(String id){
        return linkIdMap.get(id);
    }

}
