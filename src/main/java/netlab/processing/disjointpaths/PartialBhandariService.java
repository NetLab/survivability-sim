package netlab.processing.disjointpaths;

import lombok.extern.slf4j.Slf4j;
import netlab.submission.enums.Objective;
import netlab.submission.enums.ProblemClass;
import netlab.submission.request.Connections;
import netlab.submission.request.Failures;
import netlab.submission.request.NumFailsAllowed;
import netlab.submission.request.Request;
import netlab.topology.elements.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.transform.Source;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PartialBhandariService {

    private BhandariService bhandariService;

    private BellmanFordService bellmanFordService;

    @Autowired
    public PartialBhandariService(BhandariService bhandariService, BellmanFordService bellmanFordService){
        this.bhandariService = bhandariService;
        this.bellmanFordService = bellmanFordService;
    }


    public Request solve(Request request, ProblemClass problemClass, Objective objective, Topology topology, String requestSetId) {
        Map<SourceDestPair, Map<String, Path>> paths = new HashMap<>();
        Set<SourceDestPair> pairs = request.getPairs();
        Failures failCollection = request.getFailures();
        NumFailsAllowed nfaCollection = request.getNumFailsAllowed();
        Connections connCollection = request.getConnections();
        long startTime = System.nanoTime();
        switch(problemClass){
            case Flex:
                paths = pathsForFlex(pairs, failCollection.getFailureSet(),
                        nfaCollection.getTotalNumFailsAllowed(), connCollection.getNumConnections(), topology);
            //TODO: Implement Flow
            //TODO: Implement Endpoint
            //TODO: Implement FlowSharedF
            //TODO: Implement EndpointSharedF
        }
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1e9;
        log.info("Solution took: " + duration + " seconds");
        request.setChosenPaths(paths);
        request.setRunningTimeSeconds(duration);
        request.setIsFeasible(true);
        return request;
    }

    private Map<SourceDestPair,Map<String,Path>> pathsForFlex(Set<SourceDestPair> pairs, Set<Failure> failureSet,
                                                              Integer totalNumFailsAllowed, Integer numConnections, Topology topo) {

        Map<SourceDestPair, Map<String, Path>> pathMap = pairs.stream().collect(Collectors.toMap(p -> p, p -> new HashMap<>()));
        Map<Failure, Set<Path>> failureToPathMap = failureSet.stream().collect(Collectors.toMap(f -> f, f -> new HashSet<>()));
        List<SourceDestPair> sortedPairs = sortPairsByPathCost(pairs, topo);
        //Modify the topology (if necessary) by removing nodes and replacing with incoming/outgoing nodes
        boolean nodesCanFail = failureSet.stream().anyMatch(f -> f.getNode() != null);

        int totalChosenPaths = 0;
        int totalAtRiskPaths = 0;

        for(SourceDestPair pair : sortedPairs){
            List<Path> paths = findAndSortPaths(topo, pair.getSrc(), pair.getDst(),
                    numConnections, totalNumFailsAllowed, nodesCanFail, failureSet);
            // For each new path, figure out if adding it will get you any closer to goal
            // Will not get you closer if it will be disconnected by X failures shared by an existing path
            int id = 0;
            for(Path newPath : paths){
                boolean shouldBeAdded = true;
                for(Failure failure : failureSet){
                    // If this failure is in this path
                    if(determineIfFailureIsInPath(failure, newPath)){
                        // If this failure already could disconnect a chosen path, don't keep this new path
                        if(failureToPathMap.get(failure).size() > 0){
                            shouldBeAdded = false;
                            break;
                        }
                        else{
                            totalAtRiskPaths++;
                            failureToPathMap.get(failure).add(newPath);
                        }
                    }
                }
                if(shouldBeAdded){
                    pathMap.get(pair).put(String.valueOf(id), newPath);
                    id++;
                    totalChosenPaths++;
                }
                if(totalChosenPaths - Math.min(totalAtRiskPaths, totalNumFailsAllowed) >= numConnections){
                    break;
                }
            }
            if(totalChosenPaths - Math.min(totalAtRiskPaths, totalNumFailsAllowed) >= numConnections){
                break;
            }
        }
        return pathMap;
    }

    private List<Path> findAndSortPaths(Topology topology, Node src, Node dst, Integer numConnections,
                                        Integer totalNumFailsAllowed, boolean nodesCanFail, Set<Failure> failureSet){
        List<List<Link>> pathLinks = bhandariService.computeDisjointPaths(topology, src, dst,
                numConnections, totalNumFailsAllowed, nodesCanFail, failureSet, false);
        List<Path> paths = convertToPaths(pathLinks);
        return sortPathsByWeight(paths);
    }

    private boolean determineIfFailureIsInPath(Failure failure, Path newPath) {
        if(failure.getNode() != null){
            return newPath.getNodeIds().contains(failure.getNode().getId());
        }
        else{
            return newPath.getLinkIds().contains(failure.getLink().getId());
        }
    }


    private List<Path> sortPathsByWeight(List<Path> paths) {
        return paths.stream().sorted(Comparator.comparing(Path::getTotalWeight)).collect(Collectors.toList());
    }

    private List<Path> convertToPaths(List<List<Link>> pathLinks){
        return pathLinks.stream().map(Path::new).collect(Collectors.toList());
    }

    private List<SourceDestPair> sortPairsByPathCost(Set<SourceDestPair> pairs, Topology topology) {
        Map<SourceDestPair, Long> leastCostMap = new HashMap<>();
        for(SourceDestPair pair : pairs){
            List<Link> leastCostPath = bellmanFordService.shortestPath(topology, pair.getSrc(), pair.getDst());
            Long weight = leastCostPath.stream().map(Link::getWeight).reduce(0L, (li1, li2) -> li1 + li2);
            leastCostMap.put(pair, weight);
        }
        return pairs.stream().sorted(Comparator.comparing((leastCostMap::get))).collect(Collectors.toList());
    }


}
