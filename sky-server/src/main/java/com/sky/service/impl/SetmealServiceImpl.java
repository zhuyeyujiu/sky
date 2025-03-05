package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;


@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper ;
    @Autowired
    private SetmealDishMapper setmealDishMapper ;

    /**
     * 新增套餐
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {

        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmeal.setStatus(StatusConstant.DISABLE);

        // 存入数据库
        setmealMapper.insert(setmeal);

        // 获取套餐id
        Long id = setmeal.getId();

        // 存入套餐菜品关系表
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(id);
        }

        // 遍历套餐菜品关系表，为菜品id赋值
        setmealDishMapper.insertBatch(setmealDishes);

    }


    /**
     * 分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());

        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);

        log.info("套餐总数：{}", page.getTotal());
        log.info("套餐分页查询结果：{}", page.getResult());

        return new PageResult(page.getTotal(), page.getResult());
    }


    /**
     * 批量删除
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        for (Long id : ids) {
            Setmeal setmeal = setmealMapper.getById(id);
            if (setmeal.getStatus() == StatusConstant.ENABLE) {
                throw new DeletionNotAllowedException("起售中的套餐无法删除");
            }
        }
        // 删除套餐表中的数据
        setmealMapper.deleteBatch(ids);

        // 删除套餐后 关联的菜品数据也要删除
        setmealDishMapper.deleteBatchBySetmealIds(ids);

    }


    /**
     * 根据id查询套餐
     * @param id
     * @return
     */
    @Override
    public SetmealVO getByIdWithDish(Long id) {
        Setmeal setmeal = setmealMapper.getById(id);
        if (setmeal != null) {
            SetmealVO setmealVO = new SetmealVO();
            BeanUtils.copyProperties(setmeal, setmealVO);

            setmealVO.setSetmealDishes(setmealDishMapper.getByDishId(id));
            log.info("setmealVO:{}", setmealVO);
            return setmealVO;
        }
        return null;
    }


    /**
     * 修改套餐
     *
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void updateWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);

        // 删除套餐表中的菜品数据
        setmealDishMapper.deleteBatchBySetmealIds(Arrays.asList(setmealDTO.getId()));

        // 添加套餐表中的菜品数据
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(setmealDTO.getId());
        }
        setmealDishMapper.insertBatch(setmealDishes);

    }


    /**
     * 起售停售
     * @param status
     * @param id
     */
    @Override
    @Transactional
    public void startOrStop(Integer status, Long id) {
        if (status == StatusConstant.ENABLE) {
            // 起售时 如果套餐关联的菜品有一个停售 则不能起售
            List<Integer> setmealDishesStatusList = setmealMapper.getDishesStatusListBySetmealId(id);
            for (Integer setmealDishStatus : setmealDishesStatusList) {
                if (setmealDishStatus == StatusConstant.DISABLE) {
                    throw new DeletionNotAllowedException("起售失败，套餐中关联了停售的菜品");
                }
            }

        }
        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);

    }


    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }

}
