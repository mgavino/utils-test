package com.mgavino.utils_test.model;

import com.mgavino.utils.model.IdentifyEntity;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public abstract class CrudRepositoryTest<T extends IdentifyEntity> {

    @Autowired
    private MongoRepository<T, String> repository;

    protected abstract T createEntity();
    protected abstract T updateEntity(T entity);

    protected abstract void assertSave(T entity);
    protected abstract void assertUpdate(T entity);
    protected abstract void assertGet(T entity);

    @Test
    public void contextLoads() {
        Assert.assertNotNull(repository);
    }

    @Test
    public void save() throws Exception {

        T entity = repository.save( createEntity() );

        Assert.assertNotNull(entity);
        Assert.assertNotNull(entity.getId());

        assertSave(entity);

    }

    @Test
    public void update() throws Exception {

        T entity = repository.save( createEntity() );
        String id = entity.getId();

        T updatedEntity = repository.save( updateEntity(entity) );

        Assert.assertNotNull(updatedEntity);
        Assert.assertEquals(id, updatedEntity.getId());

        assertUpdate(updatedEntity);

    }

    @Test
    public void delete() throws Exception {

        T entity = repository.save( createEntity() );
        String id = entity.getId();

        repository.deleteById(id);

        Optional<T> deletedEntity = repository.findById(id);
        Assert.assertFalse(deletedEntity.isPresent());

    }

    @Test
    public void deleteAll() throws Exception {

        repository.save( createEntity() );
        repository.save( createEntity() );
        repository.save( createEntity() );

        repository.deleteAll();

        List<T> entities = repository.findAll();

        Assert.assertNotNull(entities);
        Assert.assertTrue(entities.isEmpty());

    }

    @Test
    public void get() throws Exception {

        T entity = repository.save( createEntity() );
        String id = entity.getId();

        Optional<T> savedEntityOpt = repository.findById(id);
        Assert.assertTrue(savedEntityOpt.isPresent());

        T savedEntity = savedEntityOpt.get();
        Assert.assertEquals(id, savedEntity.getId());

        assertGet(savedEntity);

    }

    @Test
    public void getAll() throws Exception {

        repository.save( createEntity() );
        repository.save( createEntity() );
        repository.save( createEntity() );

        List<T> entities = repository.findAll();

        Assert.assertNotNull(entities);
        Assert.assertTrue( entities.size() >= 3);

    }

}
