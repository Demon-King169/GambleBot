package com.motorbesitzen.gamblebot.data.repo;

import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscordMemberRepo extends CrudRepository<DiscordMember, Long> {

}
