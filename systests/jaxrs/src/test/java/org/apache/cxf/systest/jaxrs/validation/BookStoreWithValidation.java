/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.systest.jaxrs.validation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/bookstore/")
public class BookStoreWithValidation extends AbstractBookStoreWithValidation implements  BookStoreValidatable {
    private Map< String, BookWithValidation > books = new HashMap< String, BookWithValidation >();
    
    public BookStoreWithValidation() {
    }

    @GET
    @Path("/books/{bookId}")
    @Override
    public BookWithValidation getBook(@PathParam("bookId") String id) {
        return books.get(id);
    }
    
    @POST
    @Path("/books")
    public Response addBook(@Context final UriInfo uriInfo, 
            @NotNull @Size(min = 1, max = 50) @FormParam("id") String id,
            @FormParam("name") String name) {
        books.put(id, new BookWithValidation(name, id));   
        return Response.created(uriInfo.getRequestUriBuilder().path(id).build()).build();
    }
    
    @GET
    @Path("/books")
    @Override
    public Collection< BookWithValidation > list(@DefaultValue("1") @QueryParam("page") int page) {
        return books.values();
    }
    
    @DELETE
    @Path("/books")
    public Response clear() {
        books.clear();
        return Response.ok().build();
    }
}
