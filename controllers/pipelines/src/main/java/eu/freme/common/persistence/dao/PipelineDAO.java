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
package eu.freme.common.persistence.dao;

import eu.freme.common.persistence.model.Pipeline;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gerald Haesendonck
 */
@Component
public class PipelineDAO extends OwnedResourceDAO<Pipeline> {
	private final static long ONE_DAY = 24 * 60 * 60 * 1000;  // one day expressed in milliseconds

	@Override
	public String tableName() {
		return Pipeline.class.getSimpleName();
	}

	/**
	 * Deletes pipelines that are older than one week. This method runs once a day.
	 */
	@SuppressWarnings("unused")
	@Scheduled(fixedDelay = ONE_DAY)
	public void clean() {
		logger.info("Deleting pipelines older than one week!");
		long currentTime = System.currentTimeMillis();
		long oneWeek = 7 * ONE_DAY;
		List<Pipeline> pipelinesToDelete = new ArrayList<>();

		// collect pipelines older than one week
		for (Pipeline pipeline : repository.findAll()) {
			if (!pipeline.isPersist()) {
				long creationTime = pipeline.getCreationTime();
				if (currentTime - oneWeek > creationTime) {
					logger.debug("Deleted pipeline " + pipeline.getId());
					pipelinesToDelete.add(pipeline);
				}
			}
		}

		// and now delete them
		repository.delete(pipelinesToDelete);
	}
}
