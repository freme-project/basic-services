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
package eu.freme.bservices.filters.ratelimiter;

import eu.freme.bservices.filters.ratelimiter.exception.TooManyRequestsException;
import eu.freme.common.exception.InternalServerErrorException;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Jonathan Sauder (jonathan.sauder@student.hpi.de) on 18.11.15.
 */
public class RateCounterObject {


    public int index;
    public long totalSize;

    public ConcurrentLinkedQueue<Long> timestamps;
    public ConcurrentLinkedQueue<Long> sizes;


    public long max_size;
    public int max_requests;
    public long time_frame;

    public RateCounterObject(long time_frame, long timestamp, int max_requests, long size, long max_size){
        this.time_frame = time_frame;
        this.sizes=new ConcurrentLinkedQueue<>();
        this.timestamps= new ConcurrentLinkedQueue<>();
        this.index=0;
        this.totalSize=0;
        this.max_size=max_size;
        this.max_requests=max_requests;

        this.add_entry(timestamp, size);

    }

    public void add_entry(long timestamp, long size) throws TooManyRequestsException {

        if (index >= max_requests-1) {
            if (timestamps.peek()==null || timestamps.peek()!=null && timestamp - timestamps.peek() < time_frame) {
                throw new TooManyRequestsException("You exceeded the allowed "+max_requests+" requests in "+time_frame/1000+ " seconds. Please try again later.");
            }
            while (timestamps.peek() != null && timestamp - timestamps.peek() > time_frame) {
                timestamps.poll();
                totalSize -= sizes.poll();
                index--;

                //DEBUG
                if (totalSize < 0) {
                    throw new InternalServerErrorException("Something went wrong when calculating sizes of your requests");
                }
            }
        }
        if (index< max_requests -1) {
            timestamps.add(timestamp);
            sizes.add(size);
            totalSize += size;
            index++;
        }

        if (max_size!=0 && totalSize >= max_size) {
            throw new TooManyRequestsException("Your requests totalling "+totalSize+ "characters exceeded the allowed "+max_size+" characters of text. Please wait until making more requests.");
        }

    }
}
