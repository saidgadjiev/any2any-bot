package ru.gadjini.any2any.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.any2any.dao.DistributionDao;
import ru.gadjini.any2any.domain.Distribution;

import java.util.List;

@Service
public class DistributionService {

    private DistributionDao distributionDao;

    @Autowired
    public DistributionService(DistributionDao distributionDao) {
        this.distributionDao = distributionDao;
    }

    public boolean isExists() {
        return distributionDao.isExists();
    }

    public List<Distribution> popDistributions(int limit) {
        return distributionDao.popDistributions(limit);
    }

    public Distribution popDistribution(int userId) {
        return distributionDao.popDistribution(userId);
    }
}
