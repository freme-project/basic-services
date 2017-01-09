/**
 * Copyright © 2016 Agro-Know, Deutsches Forschungszentrum für Künstliche Intelligenz, iMinds,
 * Institut für Angewandte Informatik e. V. an der Universität Leipzig,
 * Istituto Superiore Mario Boella, Tilde, Vistatec, WRIPL (http://freme-project.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.freme.bservices.controllers.pipelines.core;

import java.util.Map;

/*
 * <p>A PipelineResponse with some extra stats.</p>
 * <p>
 * <p>Copyright 2015 MMLab, UGent</p>
 *
 * @author Gerald Haesendonck
 */
public class WrappedPipelineResponse {
	private final Metadata metadata;
	private final PipelineResponse content;

	public WrappedPipelineResponse(PipelineResponse content, Map<String, Long> executionTime, long totalExecutionTime) {
		this.content = content;
		metadata = new Metadata(executionTime, totalExecutionTime);
	}

	@SuppressWarnings("unused")
	public PipelineResponse getContent() {
		return content;
	}
}
