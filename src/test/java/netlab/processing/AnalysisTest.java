package netlab.processing;

import netlab.TestConfiguration;
import netlab.analysis.analyzed.AnalyzedSet;
import netlab.analysis.services.AnalysisService;
import netlab.submission.request.RequestSet;
import netlab.submission.request.SimulationParameters;
import netlab.submission.services.GenerationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class AnalysisTest {

    @Autowired
    private GenerationService generationService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ProcessingService processingService;


    @Test
    public void allFeasibleNoFailures(){
        List<Integer> minConns = Arrays.asList(1, 1);
        List<Integer> maxConns = Arrays.asList(1, 2);
        RequestSet rs = createSetWithGenService("Flex", 3, 3, 2, minConns, maxConns, 0,
                0, "Both", 0.0, 0.0, 0.0, 10);
        AnalyzedSet as = analysisService.analyzeRequestSet(rs);
        assert(as.getPercentFeasible() == 1.0);
        assert(as.getPercentSurvivableForFeasible() == 1.0);
    }

    @Test
    public void allFeasibleFlexFailures(){
        Integer numFails = 10;
        Integer nfa = 2;
        RequestSet rs = createSetWithGenService("Flex", 3, 3, 2, new ArrayList<>(),
                new ArrayList<>(), numFails, nfa, "Both", 0.0, 0.0, 0.0, 10);
        AnalyzedSet as = analysisService.analyzeRequestSet(rs);
        assert(as.getPercentFeasible() == 1.0);
        assert(as.getPercentSurvivableForFeasible() == 1.0);
    }

    @Test
    public void allFeasibleFlowSharedFFailures(){
        Integer numFails = 10;
        Integer nfa = 1;
        List<Integer> minConns = Arrays.asList(1, 1);
        List<Integer> maxConns = Arrays.asList(4, 4);
        RequestSet rs = createSetWithGenService("FlowSharedF", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Both", 0.0, 0.0, 0.0, 10);
        AnalyzedSet as = analysisService.analyzeRequestSet(rs);
        assert(as.getPercentFeasible() == 1.0);
        assert(as.getPercentSurvivableForFeasible() == 1.0);
    }

    @Test
    public void allFeasibleEndpointSharedFFailures(){
        Integer numFails = 10;
        Integer nfa = 1;
        List<Integer> minConns = Arrays.asList(1, 1);
        List<Integer> maxConns = Arrays.asList(4, 4);
        RequestSet rs = createSetWithGenService("EndpointSharedF", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Both", 0.0, 0.0, 0.0, 10);
        AnalyzedSet as = analysisService.analyzeRequestSet(rs);
        assert(as.getPercentFeasible() == 1.0);
        assert(as.getPercentSurvivableForFeasible() == 1.0);
    }

    @Test
    public void allFeasibleFlowFailures(){
        List<Integer> minConns = Arrays.asList(1, 1);
        List<Integer> maxConns = Arrays.asList(4, 4);
        Integer numFails = 10;
        Integer nfa = 1;
        RequestSet rs = createSetWithGenService("Flow", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Both", 0.0, 0.0, 0.0, 10);
        AnalyzedSet as = analysisService.analyzeRequestSet(rs);
        assert(as.getPercentFeasible() == 1.0);
        assert(as.getPercentSurvivableForFeasible() == 1.0);
    }

    @Test
    public void allFeasibleEndpointFailures(){
        List<Integer> minConns = Arrays.asList(1, 1);
        List<Integer> maxConns = Arrays.asList(4, 4);
        Integer numFails = 10;
        Integer nfa = 1;
        RequestSet rs = createSetWithGenService("Endpoint", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Both", 0.0, 0.0, 0.0, 10);
        AnalyzedSet as = analysisService.analyzeRequestSet(rs);
        assert(as.getPercentFeasible() == 1.0);
        assert(as.getPercentSurvivableForFeasible() == 1.0);
    }

    // < 100% feasible
    @Test
    public void ninetyPercentFeasibleFlexFailures(){
        List<Integer> minConns = new ArrayList<>();
        List<Integer> maxConns = new ArrayList<>();
        Integer numFails = 10;
        Integer nfa = 3;
        RequestSet rs = createSetWithGenService("Flex", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Both", 0.0, 0.0, 0.0, 10);
        AnalyzedSet as = analysisService.analyzeRequestSet(rs);
        assert(as.getPercentFeasible() == 0.9);
        assert(as.getPercentSurvivableForFeasible() == 1.0);
    }

    @Test
    public void ninetyPercentFeasibleFlowSharedFFailures(){
        Integer numFails = 24;
        Integer nfa = 2;
        List<Integer> minConns = Arrays.asList(0, 1);
        List<Integer> maxConns = Arrays.asList(4, 4);
        RequestSet rs = createSetWithGenService("FlowSharedF", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Link", 0.0, 0.0, 0.0, 10);
        AnalyzedSet as = analysisService.analyzeRequestSet(rs);
        assert(as.getPercentFeasible() == 0.9);
        assert(as.getPercentSurvivableForFeasible() == 1.0);
    }

    @Test
    public void ninetyPercentFeasibleEndpointSharedFFailures(){
        Integer numFails = 24;
        Integer nfa = 2;
        List<Integer> minConns = Arrays.asList(0, 1);
        List<Integer> maxConns = Arrays.asList(4, 4);
        RequestSet rs = createSetWithGenService("EndpointSharedF", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Link", 0.0, 0.0, 0.0, 10);
        AnalyzedSet as = analysisService.analyzeRequestSet(rs);
        assert(as.getPercentFeasible() == 0.9);
        assert(as.getPercentSurvivableForFeasible() == 1.0);
    }

    @Test
    public void halfFeasibleFlowFailures(){
        List<Integer> minConns = Arrays.asList(1, 1);
        List<Integer> maxConns = Arrays.asList(2,3);
        Integer numFails = 10;
        Integer nfa = 2;
        RequestSet rs = createSetWithGenService("Flow", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Both", 0.0, 0.0, 0.0, 10);
        AnalyzedSet as = analysisService.analyzeRequestSet(rs);
        assert(as.getPercentFeasible() == 0.5);
        assert(as.getPercentSurvivableForFeasible() == 1.0);
    }

    @Test
    public void eightyPercentFeasibleEndpointFailures(){
        Integer numFails = 5;
        Integer nfa = 3;
        List<Integer> minConns = Arrays.asList(2, 2);
        List<Integer> maxConns = Arrays.asList(4, 4);
        RequestSet rs = createSetWithGenService("Endpoint", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Link", 0.0, 0.0, 0.0, 10);
        AnalyzedSet as = analysisService.analyzeRequestSet(rs);
        assert(as.getPercentFeasible() == 0.8);
        assert(as.getPercentSurvivableForFeasible() == 1.0);
    }

    // All infeasible
    @Test
    public void infeasibleFlexFailures(){
        List<Integer> minConns = new ArrayList<>();
        List<Integer> maxConns = new ArrayList<>();
        Integer numFails = 14;
        Integer nfa = 14;
        RequestSet rs = createSetWithGenService("Flex", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Node", 0.0, 1.0, 1.0, 10);
        AnalyzedSet as = analysisService.analyzeRequestSet(rs);
        assert(as.getPercentFeasible() == 0.0);
        assert(as.getPercentSurvivableForFeasible() == 0.0);
    }

    @Test
    public void infeasibleFlowSharedFFailures(){
        Integer numFails = 14;
        Integer nfa = 14;
        List<Integer> minConns = Arrays.asList(0, 1);
        List<Integer> maxConns = Arrays.asList(4, 4);
        RequestSet rs = createSetWithGenService("FlowSharedF", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Link", 0.0, 1.0, 1.0, 10);
        AnalyzedSet as = analysisService.analyzeRequestSet(rs);
        assert(as.getPercentFeasible() == 0.0);
        assert(as.getPercentSurvivableForFeasible() == 0.0);
    }

    @Test
    public void infeasibleEndpointSharedFFailures(){
        Integer numFails = 14;
        Integer nfa = 14;
        List<Integer> minConns = Arrays.asList(0, 1);
        List<Integer> maxConns = Arrays.asList(4, 4);
        RequestSet rs = createSetWithGenService("EndpointSharedF", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Link", 0.0, 1.0, 1.0, 10);
        AnalyzedSet as = analysisService.analyzeRequestSet(rs);
        assert(as.getPercentFeasible() == 0.0);
        assert(as.getPercentSurvivableForFeasible() == 0.0);
    }

    @Test
    public void infeasibleFlowFailures(){
        List<Integer> minConns = Arrays.asList(1, 1);
        List<Integer> maxConns = Arrays.asList(2,3);
        Integer numFails = 14;
        Integer nfa = 14;
        RequestSet rs = createSetWithGenService("Flow", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Both", 0.0, 1.0, 1.0, 10);
        AnalyzedSet as = analysisService.analyzeRequestSet(rs);
        assert(as.getPercentFeasible() == 0.0);
        assert(as.getPercentSurvivableForFeasible() == 0.0);
    }

    @Test
    public void infeasibleEndpointFailures(){
        Integer numFails = 14;
        Integer nfa = 14;
        List<Integer> minConns = Arrays.asList(2, 2);
        List<Integer> maxConns = Arrays.asList(4, 4);
        RequestSet rs = createSetWithGenService("Endpoint", 3, 3, 2, minConns,
                maxConns, numFails, nfa, "Link", 0.0, 1.0, 1.0, 10);
        AnalyzedSet as = analysisService.analyzeRequestSet(rs);
        assert(as.getPercentFeasible() == 0.0);
        assert(as.getPercentSurvivableForFeasible() == 0.0);
    }

    private RequestSet createSetWithGenService(String problemClass, int numSources, int numDestinations, int numConns,
                                               List<Integer> minConns, List<Integer> maxConns, Integer numFails,
                                               Integer nfa, String failureClass, double percentSrcAlsoDest, double percentSrcFail,
                                               double percentDstFail, int numRequests) {
        return createSetAndSolve(1L, "NSFnet", numRequests, "ServiceILP", problemClass, "LinksUsed", numSources, numDestinations, numFails,
                new ArrayList<>(), failureClass, 0.0, new ArrayList<>(), numConns, minConns, maxConns,
                nfa, new ArrayList<>(), "Solo", false, false, percentSrcAlsoDest, percentSrcFail, percentDstFail);
    }

    private RequestSet createSetAndSolve(Long seed, String topologyId, Integer numRequests, String alg, String problemClass,
                                         String objective, Integer numSources, Integer numDestinations, Integer fSetSize,
                                         List<Integer> minMaxFailures, String failureClass, Double failureProb,
                                         List<Double> minMaxFailureProb, Integer numConnections,
                                         List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                                         Integer numFailsAllowed, List<Integer> minMaxFailsAllowed, String processingType, Boolean sdn,
                                         Boolean useAws, double percentSrcAlsoDest, double percentSrcFail,
                                         double percentDstFail){

        SimulationParameters params = makeParameters(seed, topologyId, numRequests, alg, problemClass, objective, numSources, numDestinations,
                fSetSize, minMaxFailures, failureClass, failureProb, minMaxFailureProb, numConnections, minConnectionsRange, maxConnectionsRange,
                numFailsAllowed, minMaxFailsAllowed, processingType, sdn, useAws, percentSrcAlsoDest, percentSrcFail, percentDstFail);
        RequestSet requestSet = generationService.generateFromSimParams(params);
        processingService.processRequestSet(requestSet);
        return requestSet;
    }

    private SimulationParameters makeParameters(Long seed, String topologyId, Integer numRequests, String alg, String problemClass,
                                                String objective, Integer numSources, Integer numDestinations, Integer fSetSize,
                                                List<Integer> minMaxFailures, String failureClass, Double failureProb,
                                                List<Double> minMaxFailureProb, Integer numConnections,
                                                List<Integer> minConnectionsRange, List<Integer> maxConnectionsRange,
                                                Integer numFails, List<Integer> minMaxFailsAllowed, String processingType, Boolean sdn,
                                                Boolean useAws, double percentSrcAlsoDest, double percentSrcFail,
                                                double percentDstFail){
        return SimulationParameters.builder()
                .seed(seed)
                .topologyId(topologyId)
                .numRequests(numRequests)
                .algorithm(alg)
                .problemClass(problemClass)
                .objective(objective)
                .numSources(numSources)
                .numDestinations(numDestinations)
                .failureSetSize(fSetSize)
                .minMaxFailures(minMaxFailures)
                .failureClass(failureClass)
                .failureProb(failureProb)
                .minMaxFailureProb(minMaxFailureProb)
                .numConnections(numConnections)
                .minConnectionsRange(minConnectionsRange)
                .maxConnectionsRange(maxConnectionsRange)
                .numFailsAllowed(numFails)
                .minMaxFailsAllowed(minMaxFailsAllowed)
                .processingType(processingType)
                .sdn(sdn)
                .useAws(useAws)
                .percentSrcAlsoDest(percentSrcAlsoDest)
                .percentSrcFail(percentSrcFail)
                .percentDestFail(percentDstFail)
                .build();
    }
}