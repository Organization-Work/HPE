package com.autonomy.find.services;

import com.autonomy.find.api.database.SearchSettings;
import com.autonomy.find.config.SearchConfig;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchSettingsService {
    @Autowired
    @Qualifier("findSessionFactory")
    private SessionFactory sessionFactory;

    @Autowired
    private SearchConfig searchConfig;


    @Transactional(readOnly = true)
    public SearchSettings getSearchSettings(final String loginUser) {
        final Session session = sessionFactory.getCurrentSession();
        SearchSettings settings = getSettings(session, loginUser);
        if (settings == null) {
            settings = new SearchSettings();
            settings.setCombine(searchConfig.getCombine());
            settings.setSummary(searchConfig.getSummary());
        }

        return settings;
    }

    @Transactional(readOnly = false)
    public SearchSettings setSearchSettings(final SearchSettings newSettings, final String loginUser) {
        final Session session = sessionFactory.getCurrentSession();
        SearchSettings existSettings = getSettings(session, loginUser);
        if (existSettings == null) {
            // create one;
            existSettings = new SearchSettings(loginUser);
        }

        existSettings.setCombine(newSettings.getCombine());
        existSettings.setSummary(newSettings.getSummary());

        if (existSettings.getId() != null) {
            session.update(existSettings);
        } else {
            session.save(existSettings);
        }

        return existSettings;
    }

    private SearchSettings getSettings(final Session session, final String owner) {
        final Query query = session.getNamedQuery("SearchSettings.getUserSettings");
        query.setString("owner", owner);

        final SearchSettings settings = (SearchSettings) query.uniqueResult();

        return settings;

    }

}
