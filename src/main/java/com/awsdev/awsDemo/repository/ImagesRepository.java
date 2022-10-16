package com.awsdev.awsDemo.repository;

import com.awsdev.awsDemo.models.ImageMetaData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImagesRepository extends JpaRepository<ImageMetaData, Integer> {
    Integer deleteByName(String name);
    List<ImageMetaData> findByName(String name);
    @Modifying
    @Query("update ImageMetaData u set u.last_update = :last_update where u.name = :name")
    void updateDate(@Param(value = "name") String name, @Param(value = "last_update") String last_update);

    @Modifying
    @Query(nativeQuery = true,
            value = "SELECT * FROM imagesMetaData.image_meta_data ORDER BY rand() LIMIT 1")
    List<ImageMetaData> randomImage();
}
