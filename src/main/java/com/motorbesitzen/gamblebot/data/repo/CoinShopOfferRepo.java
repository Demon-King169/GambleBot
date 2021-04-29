package com.motorbesitzen.gamblebot.data.repo;

import com.motorbesitzen.gamblebot.data.dao.CoinShopOffer;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoinShopOfferRepo extends CrudRepository<CoinShopOffer, Long> {
	List<CoinShopOffer> findCoinShopOffersByGuild_GuildIdOrderByPriceAsc(long guildId);
}
