package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;

public interface SetmealService {
    void saveWithDish(SetmealDTO setmealDTO);

    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    void deleteBatch(Long[] ids);

    SetmealVO getByIdWithDish(Long id);

    void updateWithDish(SetmealDTO setmealDTO);

    void startOrStop(Integer status, Long id);
}
