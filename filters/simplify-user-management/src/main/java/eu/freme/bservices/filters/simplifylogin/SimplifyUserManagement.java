package eu.freme.bservices.filters.simplifylogin;

import java.io.IOException;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.common.base.Optional;

import eu.freme.common.persistence.dao.TokenDAO;
import eu.freme.common.persistence.dao.UserDAO;
import eu.freme.common.persistence.model.Token;
import eu.freme.common.persistence.model.User;
import eu.freme.common.persistence.repository.TokenRepository;

/**
 * This filter enables simplified user login. If an API call is not
 * authenticated then it will be authenticated automatically as default_user
 * 
 * @author Jan Nehring - jan.nehring@dfki.de
 */

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SimplifyUserManagement implements Filter {

	String defaultUsername = "default_user";
	String defaultPassword = "default_password";

	@Autowired
	UserDAO userDao;

	@Autowired
	TokenRepository tokenRepository;

	@Autowired
	EntityManager em;

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest) req;
		Optional<String> tokenString = Optional.fromNullable(httpRequest.getHeader("X-Auth-Token"));

		if (tokenString.isPresent()) {
			chain.doFilter(req, res);
			return;
		}

		User user = userDao.getRepository().findOneByName(defaultUsername);
		if (user == null) {
			user = new User(defaultUsername, defaultPassword, User.roleUser);
			userDao.save(user);
		}

		Token token = null;
		if (user.getTokens().size() == 0) {
			token = new Token(UUID.randomUUID().toString(), user);
			token = tokenRepository.save(token);
		} else {
			token = user.getTokens().get(0);
		}

		OverwriteHeaderRequest newRequest = new OverwriteHeaderRequest(httpRequest, token.getToken());
		chain.doFilter(newRequest, res);
	}

	public User getDefaultUser() {
		User user = userDao.getRepository().findOneByName(defaultUsername);
		return user;
	}

	public void init(FilterConfig filterConfig) {
	}

	public void destroy() {
	}

}
