package netlab;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.AggregationParameters;
import netlab.analysis.analyzed.Analysis;
import netlab.analysis.analyzed.AnalysisParameters;
import netlab.analysis.analyzed.MassAnalysisParameters;
import netlab.analysis.controller.AnalysisController;
import netlab.analysis.services.AggregationAnalysisService;
import netlab.storage.controller.StorageController;
import netlab.submission.controller.SubmissionController;
import netlab.submission.request.Request;
import netlab.submission.request.SimulationParameters;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;


@SpringBootApplication
@EnableConfigurationProperties
@Slf4j
public class SurvivabilitySimApplication {

	public static void main(String[] args) {
		long startTime = System.nanoTime();
		Boolean webValue = false;
		SimulationParameters simParams = null;
		Long rerunSeed = null;
		AnalysisParameters analysisParams = null;
		AggregationParameters aggregationParameters = null;
		MassAnalysisParameters massAnalysisParameters = null;
		List<SimulationParameters> simParamList = null;
		boolean defaultAggregate = false;
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
		printUsage(args);
		for (String arg : args) {
			String[] splitArg = arg.split("=");
			String option = splitArg[0];
			String value = splitArg[1];
			if (option.contains("--web")) {
				webValue = Boolean.parseBoolean(value);
			}
			if (option.contains("--sim")) {
				try {
					simParams = mapper.readValue(value, SimulationParameters.class);
					log.info("Params: " + simParams.toString());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (option.contains("--rerun_incomplete")){
				rerunSeed = Long.parseLong(value);
			}
			if (option.contains("--analyze")){
				try {
					analysisParams = mapper.readValue(value, AnalysisParameters.class);
					log.info("Params: " + analysisParams.toString());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (option.contains("--aggregate")){
				try {
					aggregationParameters = mapper.readValue(value, AggregationParameters.class);
					log.info("Params: " + aggregationParameters.toString());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (option.contains("--defaultAggregate")){
				defaultAggregate = Boolean.parseBoolean(value);
			}
			if(option.contains("--massAnalyze")){
				try {
					massAnalysisParameters = mapper.readValue(value, MassAnalysisParameters.class);
					log.info("Mass Analysis Params: " + massAnalysisParameters.toString());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(option.contains("--massRun")){
				try{
					String firstId = value;
					String fileName = "scripts/input/" + firstId + "_mass.txt";
					TypeReference<List<SimulationParameters>> mapType = new TypeReference<List<SimulationParameters>>() {};
					value = new String(Files.readAllBytes(Paths.get(fileName))).replace("\n", "").replace("\r", "");
					simParamList = mapper.readValue(value, mapType);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// Set up context
		ConfigurableApplicationContext ctx = new SpringApplicationBuilder(SurvivabilitySimApplication.class)
				.web(webValue ? WebApplicationType.SERVLET : WebApplicationType.NONE)
				.run(args);

		// Get relevant controllers
		StorageController storCon =  ctx.getBean(StorageController.class);
		SubmissionController subCon = ctx.getBean(SubmissionController.class);
		AnalysisController analysCon = ctx.getBean(AnalysisController.class);

		// If they provided simulation parameters, just run the simulation, output results, and shutdown
		if(simParams != null){
			String requestId = subCon.submitRequest(simParams);
			log.info("Details ID: " + requestId);
			// If you're not analyzing the request, close the context and shut down the simulator
			if(analysisParams == null) {
				ctx.close();
				System.exit(0);
			}
		}

		// If they provided a request ID to analyze, analyze it
		if(analysisParams != null){
			log.info("Analyzing request " + analysisParams.getRequestId());
			analysCon.analyzeRequest(analysisParams);
			ctx.close();
			System.exit(0);
		}

		// If they provided a seed for rerunning incomplete params, resubmit those details
		if(rerunSeed != null){
			List<SimulationParameters> params = storCon.getParameters(rerunSeed);
			params = params.stream().filter(p -> !p.getCompleted()).collect(Collectors.toList());
			for(SimulationParameters param : params){
				subCon.submitRequest(param);
				log.info("Details Set ID: " + param.getRequestId());
			}
			ctx.close();
			System.exit(0);
		}

		// Aggregate data
		if(defaultAggregate){
			log.info("Using default parameters for aggregation");
			aggregationParameters = analysCon.getDefaultAggregateParams();
		}
		if(aggregationParameters != null){
			log.info("Aggregating Analyses");
			analysCon.aggregateWithParams(aggregationParameters);
		}

		// Mass Analysis
		if(massAnalysisParameters != null){
			log.info("Mass Analyzing");
			log.info(massAnalysisParameters.toString());
			analysCon.massAnalysis(massAnalysisParameters);
			//waitMinTime(startTime, 300);
		}

		// Mass simulation run
		if(simParamList != null){
			for(SimulationParameters simulationParameters : simParamList){
				String requestId = subCon.submitRequest(simulationParameters);
				log.info("Details ID: " + requestId);
				AnalysisParameters analysisParameters = AnalysisParameters.builder()
						.requestId(requestId)
						.useAws(simulationParameters.getUseAws())
						.build();
				analysCon.analyzeRequest(analysisParameters);
			}
			//waitMinTime(startTime, 300);
			// If you're not analyzing the request, close the context and shut down the simulator
			ctx.close();
			System.exit(0);
		}
	}

	private static void waitMinTime(long startTime, int capSeconds){
		long endTime = System.nanoTime();
		double duration = (endTime - startTime)/1e9;
		log.info("Run took: " + duration + " seconds");
		if(duration < (300)){
			try {
				Thread.sleep((long)((300 - duration) * 1000));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private static void printUsage(String[] args){
		String usage = "Usage: --server.port=<port num> --web=<true or false> " +
				"--sim={JSON representation of SimulationParameter Class} " +
				"--rerun_incomplete={seed} " +
				"--rerun_params={JSON representation of AggregationParameters Class}" +
				"--analyze={requestId='id', useAws=true/false}";
		String message = "Provided Startup Arguments: ";
		for (String arg : args) {
			message += arg + ", ";
		}
		log.info(usage);
		log.info(message);
	}


}