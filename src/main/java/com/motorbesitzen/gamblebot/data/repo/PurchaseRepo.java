package com.motorbesitzen.gamblebot.data.repo;

import com.motorbesitzen.gamblebot.data.dao.Purchase;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PurchaseRepo extends CrudRepository<Purchase, Long> {

	@Query("select p " +
			"from Purchase p " +
			"where p.guild.guildId = ?1 and p.buyer.discordId = ?2 " +
			"order by p.purchaseId desc")
	List<Purchase> findAllByGuild_GuildIdAndBuyer_DiscordIdOrderByPurchaseIdDesc(final long guildId, final long buyerId, final Pageable pageable);
}
