package com.motorbesitzen.gamblebot.data.repo;

import com.motorbesitzen.gamblebot.data.dao.DiscordGuild;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscordGuildRepo extends CrudRepository<DiscordGuild, Long> {

}
