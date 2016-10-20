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
package eu.freme.common.persistence.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.type.TypeFactory;
import eu.freme.common.exception.BadRequestException;
import eu.freme.common.exception.InternalServerErrorException;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.IOException;
import java.util.List;

/**
 * @author Gerald Haesendonck
 */
@Entity
@Table(name = "pipeline")
public class Pipeline extends OwnedResource {
	private String label;

	@Transient
	private List<PipelineRequest> requests;

	@JsonIgnore
	@Lob
	private String serializedRequests;

	private boolean persist;	// true = persist forever; false = persist for (at least) one week.

	@SuppressWarnings("unused")
	public Pipeline() {super(null);}

	@SuppressWarnings("unused")
	public List<PipelineRequest> getRequests() throws IOException {
		//deserializeRequests();
		return requests;
	}

	@SuppressWarnings("unused")
	public void setRequests(List<PipelineRequest> requests) throws JsonProcessingException {
		this.requests = requests;
		//serializeRequests();
	}

	@SuppressWarnings("unused")
	public String getLabel() {
		return label;
	}

	@SuppressWarnings("unused")
	public void setLabel(String label) {
		this.label = label;
	}

	@SuppressWarnings("unused")
	public void setPersist(boolean persist) {
		this.persist = persist;
	}

	@SuppressWarnings("unused")
	public boolean isPersist() {
		return persist;
	}

	@SuppressWarnings("unused")
	public String getSerializedRequests() throws JsonProcessingException {
		//serializeRequests();
		return serializedRequests;
	}

	@SuppressWarnings("unused")
	public void setSerializedRequests(String serializedRequests) throws IOException {
		this.serializedRequests = serializedRequests;
		//deserializeRequests();
	}

	@SuppressWarnings("unused")
	public String isValid() {
		if (label == null) {
			return "No label given.";
		}
		if (getDescription() == null) {
			return "No description given.";
		}
		if (requests == null) {
			return "No serializedRequests given.";
		}
		return "";
	}

	public void serializeRequests() throws JsonProcessingException {
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		serializedRequests = ow.writeValueAsString(requests);
	}

	public void deserializeRequests() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		requests = mapper.readValue(serializedRequests,
				TypeFactory.defaultInstance().constructCollectionType(List.class, PipelineRequest.class));
	}

	@Override
	public void preSave() {
		try {
			serializeRequests();
		} catch (JsonProcessingException e) {
			throw new BadRequestException("Could not serialize serializedRequests to json: "+e.getMessage());
		}
	}

	@Override
	public void postFetch() {
		try {
			deserializeRequests();
		} catch (IOException e) {
			throw new InternalServerErrorException("Could not deserialize serializedRequests from json: "+e.getMessage());
		}
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Pipeline pipeline = (Pipeline) o;

		if (getId() != pipeline.getId()) return false;
		if (getCreationTime() != pipeline.getCreationTime()) return false;
		if (persist != pipeline.persist) return false;
		if (!label.equals(pipeline.label)) return false;
		if (!getDescription().equals(pipeline.getDescription())) return false;
		if (getVisibility() != null ? !getVisibility().equals(pipeline.getVisibility()) : pipeline.getVisibility() != null) return false;
		if (getOwner() != null ? !getOwner().equals(pipeline.getOwner()) : pipeline.getOwner() != null) return false;
		return requests.equals(pipeline.requests);

	}

	@Override
	public int hashCode() {
		int result = (int) (getId() ^ (getId() >>> 32));
		result = 31 * result + (int) (getCreationTime() ^ (getCreationTime() >>> 32));
		result = 31 * result + label.hashCode();
		result = 31 * result + getDescription().hashCode();
		result = 31 * result + (persist ? 1 : 0);
		result = 31 * result + (getVisibility() != null ? getVisibility().hashCode() : 0);
		result = 31 * result + (getOwner() != null ? getOwner().hashCode() : 0);
		result = 31 * result + requests.hashCode();
		return result;
	}

}
