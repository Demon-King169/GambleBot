package com.motorbesitzen.gamblebot.data.repo;

import com.motorbesitzen.gamblebot.data.dao.GambleSettings;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GambleSettingsRepo extends CrudRepository<GambleSettings, Long> {

}
