package netlab.analysis.services;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.*;
import netlab.analysis.enums.MemberType;
import netlab.submission.enums.ProblemClass;
import netlab.submission.request.*;
import netlab.topology.elements.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AnalysisService {


    public AnalyzedSet analyzeRequestSet(RequestSet requestSet) {

        Map<String, Request> requestMap = requestSet.getRequests();
        Map<String, RequestMetrics> requestMetricsMap = new HashMap<>();
        for(String requestId : requestMap.keySet()){
            Request request = requestMap.get(requestId);
            RequestMetrics requestMetrics = generateMetrics(request, requestSet.getProblemClass());
            requestMetricsMap.put(requestId, requestMetrics);
        }
        return generateAnalyzedSet(requestSet, requestMetricsMap);
    }

    private AnalyzedSet generateAnalyzedSet(RequestSet requestSet, Map<String, RequestMetrics> requestMetricsMap) {

        Collection<RequestMetrics> metricsCollection = requestMetricsMap.values();
        Double totalRunningTime = 0.0;
        Double totalRunningTimeForFeasible = 0.0;
        Integer numFeasible = 0;
        Integer numRequests = metricsCollection.size();
        Integer numSurvivable = 0;
        Integer numSurvivableAndFeasible = 0;
        Integer totalLinksUsed = 0;
        Long totalCostLinksUsed = 0L;
        Integer totalNumPaths = 0;
        Integer totalDisconnectedPaths = 0;
        Integer totalIntactPaths = 0;
        Double totalAvgPathLength = 0.0;
        Double totalAvgPathCost = 0.0;
        Set<Averages> pairAverages = new HashSet<>();
        Set<Averages> srcAverages = new HashSet<>();
        Set<Averages> dstAverages = new HashSet<>();
        for(RequestMetrics metrics : metricsCollection){
            totalRunningTime += metrics.getRunningTimeSeconds();
            numSurvivable += metrics.getIsSurvivable() ? 1 : 0;
            if(metrics.getIsFeasible()){
                numSurvivableAndFeasible += metrics.getIsSurvivable() ? 1 : 0;
                totalRunningTimeForFeasible += metrics.getRunningTimeSeconds();
                numFeasible++;
                totalLinksUsed += metrics.getNumLinksUsed();
                totalCostLinksUsed += metrics.getCostLinksUsed();
                totalNumPaths += metrics.getNumPaths();
                totalDisconnectedPaths += metrics.getNumDisconnectedPaths();
                totalIntactPaths += metrics.getNumIntactPaths();
                totalAvgPathLength += metrics.getAvgPathLength();
                totalAvgPathCost += metrics.getAvgPathCost();
                pairAverages.add(metrics.getAveragesPerPair());
                srcAverages.add(metrics.getAveragesPerSrc());
                dstAverages.add(metrics.getAveragesPerDst());
            }
        }
        Averages avgPairAverages = averageSetOfAverages(pairAverages);
        Averages avgSrcAverages = averageSetOfAverages(srcAverages);
        Averages avgDstAverages = averageSetOfAverages(dstAverages);

        return AnalyzedSet.builder()
                .requestSetId(requestSet.getId())
                .seed(requestSet.getSeed())
                .problemClass(requestSet.getProblemClass())
                .algorithm(requestSet.getAlgorithm())
                .requestMetrics(requestMetricsMap)
                .numRequests(numRequests)
                .totalRunningTimeSeconds(totalRunningTime)
                .totalRunningTimeSecondsForFeasible(totalRunningTimeForFeasible)
                .avgRunningTimeSeconds(numRequests > 0 ? totalRunningTime / numRequests : 0)
                .avgRunningTimeSecondsForFeasible(numFeasible > 0 ? totalRunningTimeForFeasible / numFeasible : 0)
                .totalSurvivable(numSurvivable)
                .percentSurvivable(numRequests > 0 ? 1.0 * numSurvivable / numRequests : 0)
                .percentSurvivableForFeasible(numFeasible > 0 ? 1.0 * numSurvivableAndFeasible / numFeasible : 0)
                .totalFeasible(numFeasible)
                .percentFeasible(numRequests > 0 ? 1.0 * numFeasible / numRequests : 0)
                .totalFeasibleAndSurvivable(numSurvivableAndFeasible)
                .totalLinksUsed(totalLinksUsed)
                .avgLinksUsedForFeasible(numFeasible > 0 ? 1.0 * totalLinksUsed / numFeasible : 0)
                .totalCostLinksUsed(totalCostLinksUsed)
                .avgCostLinksUsedForFeasible(numFeasible > 0 ? 1.0 * totalCostLinksUsed / numFeasible : 0)
                .totalNumPaths(totalNumPaths)
                .avgNumPathsForFeasible(numFeasible > 0 ? 1.0 * totalNumPaths / numFeasible : 0)
                .totalDisconnectedPaths(totalDisconnectedPaths)
                .avgDisconnectedPathsForFeasible(numFeasible > 0 ? 1.0 * totalDisconnectedPaths / numFeasible : 0)
                .totalIntactPaths(totalIntactPaths)
                .avgIntactPathsForFeasible(numFeasible > 0 ? 1.0 * totalIntactPaths / numFeasible : 0)
                .avgAvgPathLength(numFeasible > 0 ? totalAvgPathLength / numFeasible : 0)
                .avgAvgPathCost(numFeasible > 0 ? totalAvgPathCost / numFeasible : 0)
                .avgAveragesPerPair(avgPairAverages)
                .avgAveragesPerSrc(avgSrcAverages)
                .avgAveragesPerDst(avgDstAverages)
                .build();
    }

    private Averages averageSetOfAverages(Set<Averages> averagesSet) {

        Integer numAverages = averagesSet.size();
        Double totalAvgPaths = 0.0;
        Double totalAvgPathsPerChosen = 0.0;
        Double totalAvgPathLength = 0.0;
        Double totalAvgPathLengthPerChosen = 0.0;
        Double totalAvgPathCost = 0.0;
        Double totalAvgPathCostPerChosen = 0.0;
        Double totalAvgDisconnectedPaths = 0.0;
        Double totalAvgDisconnectedPathsPerChosen = 0.0;
        Boolean forPair = false;
        Boolean forSource = false;
        Boolean forDest = false;
        for(Averages averages : averagesSet){
            totalAvgPaths += averages.getAvgPaths();
            totalAvgPathsPerChosen += averages.getAvgPathsPerChosen();
            totalAvgPathLength += averages.getAvgPathLength();
            totalAvgPathLengthPerChosen += averages.getAvgPathLengthPerChosen();
            totalAvgPathCost += averages.getAvgPathCost();
            totalAvgPathCostPerChosen += averages.getAvgPathCostPerChosen();
            totalAvgDisconnectedPaths += averages.getAvgDisconnectedPaths();
            totalAvgDisconnectedPathsPerChosen += averages.getAvgDisconnectedPathsPerChosen();
            forPair = averages.getForPair();
            forSource = averages.getForSource();
            forDest = averages.getForDest();
        }

        numAverages = numAverages == 0 ? 1 : numAverages;

        return Averages.builder()
                .forPair(forPair)
                .forSource(forSource)
                .forDest(forDest)
                .avgPaths(totalAvgPaths / numAverages)
                .avgPathsPerChosen(totalAvgPathsPerChosen / numAverages)
                .avgPathLength(totalAvgPathLength / numAverages)
                .avgPathLengthPerChosen(totalAvgPathLengthPerChosen / numAverages)
                .avgPathCost(totalAvgPathCost / numAverages)
                .avgPathCostPerChosen(totalAvgPathCostPerChosen / numAverages)
                .avgDisconnectedPaths(totalAvgDisconnectedPaths / numAverages)
                .avgDisconnectedPathsPerChosen(totalAvgDisconnectedPathsPerChosen / numAverages)
                .build();
    }

    private RequestMetrics generateMetrics(Request request, ProblemClass problemClass) {
        Map<SourceDestPair, Map<String, Path>> chosenPaths = request.getChosenPaths();
        Set<SourceDestPair> pairs = request.getPairs();
        Set<Node> sources = request.getSources();
        Set<Node> destinations = request.getDestinations();
        Connections connectionColl = request.getConnections();
        Failures failureColl = request.getFailures();
        Integer numConnections = connectionColl.getNumConnections();

        Map<SourceDestPair, PathSetMetrics> pathSetMetricsMap = new HashMap<>();
        Map<MemberType, Map<Node, Map<SourceDestPair, PathSetMetrics>>> memberPathSetMetricsMap = new HashMap<>();
        Boolean requestIsSurvivable = true;
        Integer numLinkUsages = 0;
        Integer numFailed = 0;
        Integer numPaths = 0;
        Long totalPathCost = 0L;

        // Check the high-level attributes
        if(problemClass.equals(ProblemClass.Flex) || problemClass.equals(ProblemClass.EndpointSharedF) || problemClass.equals(ProblemClass.FlowSharedF)){
            List<List<Failure>> failureGroups = failureColl.getFailureGroups();
            Map<Node, List<List<Failure>>> srcFailureGroupsMap = sources.stream().collect(Collectors.toMap(s -> s, s -> failureGroups));
            Map<Node, List<List<Failure>>> dstFailureGroupsMap = destinations.stream().collect(Collectors.toMap(d -> d, d -> failureGroups));

            // Must analyze paths based on total paths - choose just one failure group for everybody
            pathSetMetricsMap = analyzeAllPaths(chosenPaths, pairs, failureGroups);
            for(PathSetMetrics psm : pathSetMetricsMap.values()){
                numLinkUsages += psm.getNumLinkUsages();
                numFailed += psm.getNumFailed();
                numPaths += psm.getNumPaths();
                totalPathCost += psm.getTotalLinkCost();
            }
            memberPathSetMetricsMap = analyzeAllPathsForEndpoints(chosenPaths, sources, destinations, pairs, srcFailureGroupsMap, dstFailureGroupsMap);
        }
        // Check the flow-level attributes
        if(problemClass.equals(ProblemClass.Flow)){
            Map<SourceDestPair, Integer> minNumConnectionsMap = connectionColl.getPairMinConnectionsMap();
            Map<SourceDestPair, List<List<Failure>>> failuresMap = failureColl.getPairFailureGroupsMap();
            Map<Node, List<List<Failure>>> srcFailureGroupsMap = new HashMap<>();
            Map<Node, List<List<Failure>>> dstFailureGroupsMap = new HashMap<>();
            // Must analyze number of paths based on pairs
            for(SourceDestPair pair : pairs){
                Map<String, Path> pathMap = chosenPaths.get(pair);
                numPaths += pathMap.values().size();

                Integer minConn = minNumConnectionsMap.get(pair);

                List<List<Failure>> failureGroups = failuresMap.get(pair);
                srcFailureGroupsMap.putIfAbsent(pair.getSrc(), new ArrayList<>());
                srcFailureGroupsMap.get(pair.getSrc()).addAll(failureGroups);
                dstFailureGroupsMap.putIfAbsent(pair.getDst(), new ArrayList<>());
                dstFailureGroupsMap.get(pair.getDst()).addAll(failureGroups);

                PathSetMetrics pathSetMetrics = getWorstCasePathSetMetrics(pathMap, failureGroups);
                numLinkUsages += pathSetMetrics.getNumLinkUsages();
                numFailed += pathSetMetrics.getNumFailed();
                totalPathCost += pathSetMetrics.getTotalLinkCost();
                pathSetMetricsMap.put(pair, pathSetMetrics);

                if(pathMap.values().size() - pathSetMetrics.getNumFailed() < minConn){
                    requestIsSurvivable = false;
                }
            }
            memberPathSetMetricsMap = analyzeAllPathsForEndpoints(chosenPaths, sources, destinations, pairs, srcFailureGroupsMap, dstFailureGroupsMap);
        }
        if(problemClass.equals(ProblemClass.Endpoint)){
            Map<Node, List<List<Failure>>> srcFailureGroupsMap = failureColl.getSrcFailureGroupsMap();
            Map<Node, List<List<Failure>>> dstFailureGroupMap = failureColl.getDstFailureGroupsMap();
            memberPathSetMetricsMap = analyzeAllPathsForEndpoints(chosenPaths, sources, destinations, pairs, srcFailureGroupsMap, dstFailureGroupMap);
            List<Path> paths = chosenPaths.values().stream().map(Map::values).flatMap(Collection::stream).collect(Collectors.toList());
            numPaths = paths.size();
            numLinkUsages = paths.stream().mapToInt(p -> p.getLinks().size()).sum();
            totalPathCost += paths.stream().map(Path::getLinks).flatMap(Collection::stream).mapToLong(Link::getWeight).sum();
            numFailed = determineMaxNumFailedAcrossMemberTypes(memberPathSetMetricsMap);

            // Get metrics for pairs
            for(SourceDestPair pair : pairs){
                List<List<Failure>> failureGroups = srcFailureGroupsMap.get(pair.getSrc());
                failureGroups.addAll(dstFailureGroupMap.get(pair.getDst()));
                PathSetMetrics psm =  getWorstCasePathSetMetrics(chosenPaths.get(pair), failureGroups);
                pathSetMetricsMap.put(pair, psm);
            }
        }

        if(numPaths - numFailed < numConnections){
            requestIsSurvivable = false;
        }

        Double avgPathLength = numPaths > 0 ? numLinkUsages / numPaths * 1.0 : 0.0;
        Double avgPathCost = numPaths > 0 ? totalPathCost / numPaths * 1.0 : 0.0;
        Averages pairAverages = getAveragesForPairs(pairs, pathSetMetricsMap);
        Averages srcAverages = getAveragesForMember(sources, memberPathSetMetricsMap, true);
        Averages dstAverages = getAveragesForMember(destinations, memberPathSetMetricsMap, false);
        return RequestMetrics.builder()
                .requestId(request.getId())
                .isSurvivable(requestIsSurvivable)
                .isFeasible(request.getIsFeasible())
                .runningTimeSeconds(request.getRunningTimeSeconds())
                .numLinksUsed(numLinkUsages)
                .costLinksUsed(totalPathCost)
                .numPaths(numPaths)
                .numDisconnectedPaths(numFailed)
                .numIntactPaths(numPaths - numFailed)
                .avgPathLength(avgPathLength)
                .avgPathCost(avgPathCost)
                .averagesPerPair(pairAverages)
                .averagesPerSrc(srcAverages)
                .averagesPerDst(dstAverages)
                .pathSetMetricsMap(pathSetMetricsMap)
                .memberPathSetMetricsMap(memberPathSetMetricsMap)
                .build();
    }

    private Averages getAveragesForMember(Set<Node> members,
                                          Map<MemberType, Map<Node, Map<SourceDestPair, PathSetMetrics>>> memberPathSetMetricsMap,
                                          boolean isSource) {
        Double totalPaths = 0.0;
        Double totalPathLength = 0.0;
        Double totalPathCost = 0.0;
        Double totalDisconnectedPaths = 0.0;
        Integer numChosen = 0;
        Map<Node, Map<SourceDestPair, PathSetMetrics>> memberMap = isSource ? memberPathSetMetricsMap.get(MemberType.Source) : memberPathSetMetricsMap.get(MemberType.Destination);
        for(Node member: members){
            boolean isUsed = false;
            for(SourceDestPair pair : memberMap.get(member).keySet()) {
                PathSetMetrics psm = memberMap.get(member).get(pair);
                totalPaths += psm.getNumPaths();
                totalPathLength += psm.getNumLinkUsages();
                totalPathCost += psm.getTotalLinkCost();
                totalDisconnectedPaths += psm.getNumFailed();
                if(psm.getChosen()){
                    isUsed = true;
                }
            }
            if(isUsed){
                numChosen++;
            }
        }
        return Averages.builder()
                .avgPaths(totalPaths / members.size())
                .avgPathLength(totalPathLength / members.size())
                .avgPathCost(totalPathCost / members.size())
                .avgDisconnectedPaths(totalDisconnectedPaths / members.size())
                .avgPathsPerChosen(totalPaths / numChosen)
                .avgPathLengthPerChosen(totalPathLength / numChosen)
                .avgPathCostPerChosen(totalPathCost / numChosen)
                .avgDisconnectedPathsPerChosen(totalDisconnectedPaths / members.size())
                .forPair(false)
                .forSource(isSource)
                .forDest(!isSource)
                .build();
    }

    private Averages getAveragesForPairs(Set<SourceDestPair> pairs, Map<SourceDestPair, PathSetMetrics> pathSetMetricsMap){

        Double totalPaths = 0.0;
        Double totalPathLength = 0.0;
        Double totalPathCost = 0.0;
        Double totalDisconnectedPaths = 0.0;
        Integer numChosen = 0;
        for(SourceDestPair pair: pairs){
            PathSetMetrics psm = pathSetMetricsMap.get(pair);
            totalPaths += psm.getNumPaths();
            totalPathLength += psm.getNumLinkUsages();
            totalPathCost += psm.getTotalLinkCost();
            totalDisconnectedPaths += psm.getNumFailed();
            if(psm.getChosen()){
                numChosen++;
            }
        }
        return Averages.builder()
                .avgPaths(totalPaths / pairs.size())
                .avgPathLength(totalPathLength / pairs.size())
                .avgPathCost(totalPathCost / pairs.size())
                .avgDisconnectedPaths(totalDisconnectedPaths / pairs.size())
                .avgPathsPerChosen(numChosen > 0 ? totalPaths / numChosen : 0)
                .avgPathLengthPerChosen(numChosen > 0 ? totalPathLength / numChosen : 0)
                .avgPathCostPerChosen(numChosen > 0 ? totalPathCost / numChosen : 0)
                .avgDisconnectedPathsPerChosen(numChosen > 0 ?  totalDisconnectedPaths / pairs.size() : 0)
                .forPair(true)
                .forSource(false)
                .forDest(false)
                .build();
    }

    private Integer determineMaxNumFailedAcrossMemberTypes(Map<MemberType, Map<Node, Map<SourceDestPair, PathSetMetrics>>> memberPathSetMetricsMap) {
        Integer maxSum = 0;
        for(MemberType memberType : memberPathSetMetricsMap.keySet()){
            Map<Node, Map<SourceDestPair, PathSetMetrics>> memberMetricsMap = memberPathSetMetricsMap.get(memberType);
            Integer memberSum = 0;
            for(Node node : memberMetricsMap.keySet()){
                Map<SourceDestPair, PathSetMetrics> metricsMap = memberMetricsMap.get(node);
                memberSum +=  metricsMap.values().stream().mapToInt(PathSetMetrics::getNumFailed).sum();
            }
            maxSum = maxSum < memberSum ? memberSum : maxSum;
        }
        return maxSum;
    }

    private Map<MemberType, Map<Node, Map<SourceDestPair, PathSetMetrics>>> analyzeAllPathsForEndpoints(Map<SourceDestPair, Map<String, Path>> chosenPaths,
                                                                                        Set<Node> sources, Set<Node> destinations,
                                                                                        Set<SourceDestPair> pairs,
                                                                                        Map<Node, List<List<Failure>>> srcFailureGroupsMap,
                                                                                        Map<Node, List<List<Failure>>> dstFailureGroupMap) {
        // Must analyze number of paths based on source/dest
        // Pick worst case failure group per member
        Map<Node, Map<SourceDestPair, PathSetMetrics>> metricsBySource = getPathSetMetricsPerMember(sources, pairs, srcFailureGroupsMap, chosenPaths, true);
        Map<Node, Map<SourceDestPair, PathSetMetrics>> metricsByDest = getPathSetMetricsPerMember(destinations, pairs, dstFailureGroupMap, chosenPaths, false);

        Map<MemberType, Map<Node, Map<SourceDestPair, PathSetMetrics>>> memberMetricsMap = new HashMap<>();
        memberMetricsMap.put(MemberType.Source, metricsBySource);
        memberMetricsMap.put(MemberType.Destination, metricsByDest);
        return memberMetricsMap;
    }

    private Map<Node, Map<SourceDestPair, PathSetMetrics>> getPathSetMetricsPerMember(Set<Node> members, Set<SourceDestPair> pairs,
                                                                Map<Node, List<List<Failure>>> srcFailureGroupsMap,
                                                                Map<SourceDestPair, Map<String, Path>> chosenPaths, boolean bySource) {
        Map<Node, Set<SourceDestPair>> pairsByMemberMap = getPairsByMember(pairs, bySource);
        Map<Node, Map<SourceDestPair, PathSetMetrics>> metricsMap = new HashMap<>();
        // Must analyze number of paths based on source/dest
        // Pick worst case failure group per member
        for(Node member : members){
            List<List<Failure>> failureGroups = srcFailureGroupsMap.get(member);
            Set<SourceDestPair> pairsWithSrc = pairsByMemberMap.get(member);
            // This gives us the metrics for the worst-case failure group
            Map<SourceDestPair, PathSetMetrics> pathSetMetricsMap = analyzeAllPaths(chosenPaths, pairsWithSrc, failureGroups);
            metricsMap.put(member, pathSetMetricsMap);
        }
        return metricsMap;
    }

    private Map<Node, Set<SourceDestPair>> getPairsByMember(Set<SourceDestPair> pairs, boolean bySource) {
        Map<Node, Set<SourceDestPair>> pairMap = new HashMap<>();
        for(SourceDestPair pair : pairs){
            Node member = bySource ? pair.getSrc() : pair.getDst();
            pairMap.putIfAbsent(member, new HashSet<>());
            pairMap.get(member).add(pair);
        }
        return pairMap;
    }

    private Map<SourceDestPair, PathSetMetrics> analyzeAllPaths(Map<SourceDestPair, Map<String, Path>> chosenPaths, Set<SourceDestPair> pairs,
                                 List<List<Failure>> failureGroups) {
        Map<SourceDestPair, PathSetMetrics> worstCasePathSetMap = new HashMap<>();
        Integer worstNumFailed = 0;

        for(List<Failure> failureGroup : failureGroups){
            Integer numFailedTemp = 0;
            Map<SourceDestPair, PathSetMetrics> tempMap = new HashMap<>();
            for(SourceDestPair pair : pairs){
                Map<String, Path> pathMap = chosenPaths.get(pair);

                PathSetMetrics pathSetMetrics = getMetricsForPathSet(pathMap, failureGroup);

                numFailedTemp += pathSetMetrics.getNumFailed();
                tempMap.put(pair, pathSetMetrics);
            }
            if(numFailedTemp > worstNumFailed){
                worstCasePathSetMap = new HashMap<>(tempMap);
                worstNumFailed = numFailedTemp;
            }
        }
        if(worstCasePathSetMap.isEmpty()){
            for(SourceDestPair pair : pairs){
                Map<String, Path> pathMap = chosenPaths.get(pair);
                PathSetMetrics pathSetMetrics = getMetricsForPathSet(pathMap, new ArrayList<>());
                worstNumFailed += pathSetMetrics.getNumFailed();
                worstCasePathSetMap.put(pair, pathSetMetrics);
            }
        }

        return worstCasePathSetMap;
    }

    private PathSetMetrics getWorstCasePathSetMetrics(Map<String, Path> pathMap, List<List<Failure>> failureGroups){
        PathSetMetrics worstPathSetMetrics = null;
        for(List<Failure> group : failureGroups){
            PathSetMetrics pathSetMetrics = getMetricsForPathSet(pathMap, group);
            if(worstPathSetMetrics == null || worstPathSetMetrics.getNumFailed() < pathSetMetrics.getNumFailed()){
                worstPathSetMetrics = pathSetMetrics;
            }
        }
        if(worstPathSetMetrics == null){
            worstPathSetMetrics = getMetricsForPathSet(pathMap, new ArrayList<>());
        }
        return worstPathSetMetrics;
    }

    private PathSetMetrics getMetricsForPathSet(Map<String, Path> pathMap, List<Failure> group){
        Map<String, PathMetrics> pathMetrics = getPathMetrics(pathMap, group);
        return analyzePathMetrics(pathMetrics);
    }

    private Map<String, PathMetrics> getPathMetrics(Map<String, Path> pathMap, List<Failure> failures) {
        Map<String, PathMetrics> pathMetricsMap = new HashMap<>();
        for(String pathId : pathMap.keySet()){
            Path path = pathMap.get(pathId);
            Integer numLinks = path.getLinks().size();
            Long totalCost = path.getLinks().stream().mapToLong(Link::getWeight).sum();
            Boolean survived = testSurvival(path, failures);
            PathMetrics metrics = PathMetrics.builder().numLinks(numLinks).survived(survived).cost(totalCost).build();
            pathMetricsMap.put(pathId, metrics);
        }
        return pathMetricsMap;
    }

    private Boolean testSurvival(Path path, List<Failure> failures) {
        Set<String> nodeIds = path.getNodeIds();
        Set<String> linkIds = path.getLinkIds();

        for(Failure failure : failures){
            if(failure.getNode() != null){
                if(nodeIds.contains(failure.getNode().getId())){
                    return false;
                }
            }
            if(failure.getLink() != null){
                if(linkIds.contains(failure.getLink().getId()) || linkIds.contains(invertLinkId(failure.getLink()))){
                    return false;
                }
            }
        }
        return true;
    }

    private String invertLinkId(Link link) {
        return link.getTarget().getId() + "-" + link.getOrigin().getId();
    }

    private PathSetMetrics analyzePathMetrics(Map<String, PathMetrics> pathMetricsMap) {

        Integer numLinkUsages = 0;
        Integer numFailed = 0;
        Integer numPaths = 0;
        Long totalCost = 0L;
        Boolean chosen = false;
        if(!pathMetricsMap.isEmpty()) {
            chosen = true;
            for (PathMetrics pathMetrics : pathMetricsMap.values()) {
                numLinkUsages += pathMetrics.getNumLinks();
                if (!pathMetrics.getSurvived()) {
                    numFailed++;
                }
                numPaths++;
                totalCost += pathMetrics.getCost();
            }
        }

        return PathSetMetrics.builder()
                .pathMetricsMap(pathMetricsMap)
                .numLinkUsages(numLinkUsages)
                .numFailed(numFailed)
                .numPaths(numPaths)
                .totalLinkCost(totalCost)
                .chosen(chosen)
                .build();
    }


}
