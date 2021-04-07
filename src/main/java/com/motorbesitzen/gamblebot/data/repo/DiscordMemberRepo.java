package com.motorbesitzen.gamblebot.data.repo;

import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiscordMemberRepo extends CrudRepository<DiscordMember, Long> {

	Optional<DiscordMember> findByDiscordIdAndGuild_GuildId(final long discordId, final long guildId);
}
