package com.motorbesitzen.gamblebot.data.repo;

import com.motorbesitzen.gamblebot.data.dao.DiscordMember;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscordMemberRepo extends CrudRepository<DiscordMember, Long> {

	Optional<DiscordMember> findByDiscordIdAndGuild_GuildId(final long discordId, final long guildId);

	@Query("select d " +
			"from DiscordMember d " +
			"where d.guild.guildId = ?1 and d.coins > 0 and d.gamesPlayed >= 10 " +
			"order by d.coins desc, d.coinsWon desc, d.id asc")
	List<DiscordMember> findAllByGuild_GuildIdOrderByCoinsDesc(final long guildId, final Pageable pageable);
}
