package com.mgavino.utils_test.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.mgavino.utils.controller.CrudController;
import com.mgavino.utils.model.IdentifyEntity;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;

public abstract class CrudControllerTest<T extends IdentifyEntity> {

    @Autowired
    private CrudController<T> controller;

    @Autowired
    private MongoRepository<T, String> repository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Class<T> entityTypeClass;

    protected abstract String getUri();

    protected abstract T createEntity();
    protected abstract T updatePutEntity(T entity);
    protected abstract T createPathEntity();

    protected abstract void assertPost(T entity);
    protected abstract void assertPut(T entity);
    protected abstract void assertPath(T entity);
    protected abstract void assertGet(T entity);

    @Before
    public void setUp() throws Exception {

        ParameterizedType pt = (ParameterizedType) getClass().getGenericSuperclass();
        // You may need this split or not, use logging to check
        String parameterClassName = pt.getActualTypeArguments()[0].toString().split("\\s")[1];
        // Instantiate the Parameter and initialize it.
        entityTypeClass = (Class<T>) Class.forName(parameterClassName);

    }

    @Test
    public void contextLoads() {
        Assert.assertNotNull(controller);
    }

    @Test
    public void post() throws Exception {

        T entity = createEntity();

        String location = mockMvc.perform(
            MockMvcRequestBuilders.post( getUri() )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(entity)))
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.header().exists("location"))
            .andReturn().getResponse().getHeader("location");

        String id = location.substring( location.lastIndexOf("/") + 1 );
        Optional<T> savedEntityOpt = repository.findById(id);
        Assert.assertTrue(savedEntityOpt.isPresent());

        T savedEntity = savedEntityOpt.get();
        assertPost(savedEntity);

    }

    @Test
    public void put() throws Exception {

        T entity = repository.save( createEntity() );
        String id = entity.getId();

        entity.setId(null);
        updatePutEntity(entity);

        mockMvc.perform(
            MockMvcRequestBuilders.put(getUri() + "/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(entity)))
            .andExpect(MockMvcResultMatchers.status().isOk());

        Optional<T> updatedEntityOpt = repository.findById(id);
        Assert.assertTrue(updatedEntityOpt.isPresent());

        T updatedEntity = updatedEntityOpt.get();
        assertPut(updatedEntity);

    }

    @Test
    public void patch() throws Exception {

        T entity = repository.save( createEntity() );
        String id = entity.getId();

        T patchEntity = createPathEntity();

        mockMvc.perform(
                MockMvcRequestBuilders.patch(getUri() + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchEntity)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        Optional<T> updatedEntityOpt = repository.findById(id);
        Assert.assertTrue(updatedEntityOpt.isPresent());

        T updatedEntity = updatedEntityOpt.get();
        assertPath(updatedEntity);

    }

    @Test
    public void delete() throws Exception {

        T entity = repository.save( createEntity() );
        String id = entity.getId();

        mockMvc.perform(
                MockMvcRequestBuilders.delete(getUri() + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        Optional<T> deleteEntity = repository.findById(id);
        Assert.assertFalse(deleteEntity.isPresent());

    }

    @Test
    public void get() throws Exception {

        T entity = repository.save( createEntity() );
        String id = entity.getId();

        String contentResponse = mockMvc.perform(
                                    MockMvcRequestBuilders.get(getUri() + "/" + id)
                                            .contentType(MediaType.APPLICATION_JSON))
                                    .andExpect(MockMvcResultMatchers.status().isOk())
                                    .andExpect(MockMvcResultMatchers.content().string( Matchers.notNullValue() ))
                                    .andReturn().getResponse().getContentAsString();

        T entityResponse = objectMapper.reader().forType(entityTypeClass).readValue(contentResponse);
        Assert.assertNotNull(entityResponse);
        Assert.assertEquals(id, entityResponse.getId());
        assertGet(entityResponse);

    }

    @Test
    public void getAll() throws Exception {

        repository.deleteAll();

        repository.save( createEntity() );
        repository.save( createEntity() );
        repository.save( createEntity() );

        String contentResponse = mockMvc.perform(
                MockMvcRequestBuilders.get(getUri())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string( Matchers.notNullValue() ))
                .andReturn().getResponse().getContentAsString();

        CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, entityTypeClass);
        List<T> entitiesResponse = objectMapper.reader().forType(collectionType).readValue(contentResponse);

        Assert.assertNotNull(entitiesResponse);
        Assert.assertEquals(3, entitiesResponse.size());

    }

}
