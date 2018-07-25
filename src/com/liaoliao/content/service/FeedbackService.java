package com.liaoliao.content.service;

import java.util.List;

import com.liaoliao.content.entity.Feedback;

public interface FeedbackService {
	
	void saveFeedback(Feedback feedback);

	Integer findCount();

	List<Feedback> findAll(Integer pageNo);

	Integer findCountByStatus(Integer status);

	List<Feedback> findAllByStatus(Integer pageNo, Integer status);

	Feedback findById(Integer id);

	void updateFB(Feedback fd);

	/**
	 * 根据用户id查询查询用户反馈信息
	 * @param userId
	 * @return
	 */
	List<Feedback> findByUserId(Integer userId);


}
