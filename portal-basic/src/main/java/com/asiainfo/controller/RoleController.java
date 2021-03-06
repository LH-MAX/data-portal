package com.asiainfo.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.asiainfo.dto.PageDto;
import com.asiainfo.dto.RestResponse;
import com.asiainfo.entity.Platform;
import com.asiainfo.entity.Role;
import com.asiainfo.exception.BadRequestException;
import com.asiainfo.exception.ResourceNotFoundException;
import com.asiainfo.operlog.BusinessType;
import com.asiainfo.operlog.Log;
import com.asiainfo.service.RoleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.hibernate.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Api(tags = "角色管理")
@RestController
@RequestMapping("/roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @Log(title = "新增角色", action = BusinessType.INSERT)
    @ApiOperation(value = "新增角色",notes = "新增角色")
    @PostMapping
    public RestResponse save(@Valid @RequestBody Role role){
        role.setOpTime(new Date());
        roleService.save(role);
        return new RestResponse("添加成功");
    }

    @Log(title = "更新角色", action = BusinessType.UPDATE)
    @ApiOperation(value = "更新角色",notes = "更新角色")
    @PutMapping
    public RestResponse update(@RequestBody Role role) throws BadRequestException, ResourceNotFoundException {
        if(role.getRoleId() == null){
            throw new BadRequestException("请求参数缺少角色ID");
        }
        Role source = roleService.getRoleById(role.getRoleId());
        if(source == null){
            throw new ResourceNotFoundException("没有找到要修改的角色");
        }
        int update = roleService.update(role);
        if(update == 1){
            return new RestResponse("操作成功");
        }
        return new RestResponse("操作成功");
    }

    @Log(title = "删除角色", action = BusinessType.DELETE)
    @ApiOperation(value = "删除角色",notes = "删除角色")
    @DeleteMapping("/{id}")
    public RestResponse delete(@PathVariable Long id) throws ResourceNotFoundException, BadRequestException {
        Role role = roleService.getRoleById(id);
        if(role == null){
            throw new ResourceNotFoundException("找不到要删除的角色！");
        }
        roleService.delete(id);
        return new RestResponse("删除成功");
    }

    @Log(title = "是否激活角色", action = BusinessType.ENABLE_FORBIDDEN)
    @ApiOperation(value = "是否激活角色",notes = "是否激活角色")
    @PostMapping("/isAvailable")
    public RestResponse isAvailable(@RequestBody Role role) throws BadRequestException, ResourceNotFoundException {
        if(role.getRoleId() == null){
            throw new BadRequestException("请求参数缺少角色ID");
        }
        Role source = roleService.getRoleById(role.getRoleId());
        if(source == null){
            throw new ResourceNotFoundException("没有找到要修改的角色！");
        }
        Role operate = new Role();
        operate.setRoleId(role.getRoleId());
        operate.setRoleValid(role.getRoleValid());
        int update = roleService.update(operate);
        if(update == 1){
            return new RestResponse("操作成功");
        }
        return new RestResponse("操作成功");
    }


    @Log(title = "查询角色列表-分页", action = BusinessType.SELECT)
    @ApiOperation(value = "查询角色列表-分页",notes = "查询角色列表-分页")
    @GetMapping("/queryRoleList")
    public RestResponse getPublic(@RequestParam Integer pageNum,
                                  @RequestParam Integer pageSize,
                                  @RequestParam(required=false) String roleName){

        PageDto pageDto = roleService.queryRoleList(pageNum, pageSize, roleName);
        return new RestResponse(pageDto,"查询成功");
    }

    @Log(title = "批量操作", action = BusinessType.UPDATE)
    @ApiOperation(value = "批量操作",notes = "批量操作（激活、冻结、批量删除）")
    @PutMapping("/batch")
    public RestResponse batch(@RequestBody JSONObject param) throws BadRequestException {

        String operate = param.getString("operate");
        if(StringUtils.isEmpty(operate)){
            throw new BadRequestException("请选择操作类型");
        }

        JSONArray ids = param.getJSONArray("ids");
        List<Long> roleIds = ids.toJavaList(Long.class);
        if(roleIds.size() == 0){
            throw new BadRequestException("请选择角色");
        }

        if("delete".equals(operate)){
            int i = roleService.batchDelete(roleIds);
            return new RestResponse("删除成功数量："+i);
        }else if("valid".equals(operate)){
            Boolean valid = param.getBoolean("valid");
            int i = roleService.batchUpdate(roleIds, valid);
            return new RestResponse("修改成功数量："+i);
        }else{
            throw new BadRequestException("没有找到你选的操作");
        }
    }

    @Log(title = "角色授权", action = BusinessType.UPDATE)
    @ApiOperation(value = "角色授权",notes = "角色授权")
    @PostMapping("/authorize")
    public RestResponse authorize(@RequestBody JSONObject param) throws BadRequestException {
        Long roleId = param.getLong("roleId");
        Long platformId = param.getLong("paltformId");
        if(roleId == null){
            throw new BadRequestException("请选择角色");
        }
        JSONArray tenantIds = param.getJSONArray("tenantIds");
        JSONArray userIds = param.getJSONArray("userIds");
        if(tenantIds == null && userIds == null){
            throw new BadRequestException("请选择用户");
        }
        List<Long> tenants = new ArrayList<>();
        if(tenantIds != null){
            tenants = tenantIds.toJavaList(Long.class);
        }
        List<Long> users = new ArrayList<>();
        if(userIds != null){
            users = userIds.toJavaList(Long.class);
        }
        roleService.authorize(roleId, platformId, users, tenants);

        return new RestResponse("操作成功");
    }

    @Log(title = "查询角色下用户", action = BusinessType.SELECT)
    @ApiOperation(value = "查询角色下用户",notes = "查询角色下用户")
    @GetMapping("/queryUser")
    public RestResponse queryUserByRoleId(@RequestParam Long roleId,
                                          @RequestParam String type) throws BadRequestException {
        if(!"user".equals(type) && !"tenant".equals(type)){
            throw new BadRequestException("没有类型查询");
        }
        List<Long> users = roleService.queryUserByRoleId(roleId,type);
        return new RestResponse(users,"查询成功");
    }

}
