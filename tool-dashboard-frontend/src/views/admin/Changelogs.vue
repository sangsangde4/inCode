<template>
  <div class="admin-changelogs">
    <div class="page-header">
      <div class="header-left">
        <h2>变更日志管理</h2>
        <el-tag type="info" size="large">总计 {{ total }} 条日志</el-tag>
      </div>
      <el-button type="primary" size="large" @click="handleAdd" class="add-btn">
        <el-icon><Plus /></el-icon>
        新增日志
      </el-button>
    </div>

    <el-card class="table-card" shadow="never">

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="toolId" label="工具ID" width="100" />
        <el-table-column prop="version" label="版本" width="120" />
        <el-table-column prop="changeType" label="变更类型" width="100" />
        <el-table-column prop="content" label="变更内容" show-overflow-tooltip />
        <el-table-column prop="changer" label="变更人" width="120" />
        <el-table-column prop="changeTime" label="变更时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="关联工具" prop="toolId">
          <el-select v-model="form.toolId" style="width: 100%" filterable>
            <el-option
              v-for="tool in tools"
              :key="tool.id"
              :label="tool.name"
              :value="tool.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="版本号" prop="version">
          <el-input v-model="form.version" />
        </el-form-item>
        <el-form-item label="变更类型" prop="changeType">
          <el-select v-model="form.changeType" style="width: 100%">
            <el-option label="新增" value="新增" />
            <el-option label="修复" value="修复" />
            <el-option label="优化" value="优化" />
            <el-option label="删除" value="删除" />
          </el-select>
        </el-form-item>
        <el-form-item label="变更内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item label="变更人" prop="changer">
          <el-input v-model="form.changer" />
        </el-form-item>
        <el-form-item label="变更时间" prop="changeTime">
          <el-date-picker
            v-model="form.changeTime"
            type="datetime"
            placeholder="选择变更时间"
            style="width: 100%"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getLogPage, addChangeLog, updateChangeLog, deleteChangeLog } from '@/api/changelog'
import { getToolList } from '@/api/tool'
import type { ChangeLog, Tool } from '@/types'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增日志')
const formRef = ref<FormInstance>()

const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref<ChangeLog[]>([])
const tools = ref<Tool[]>([])

const form = reactive<ChangeLog>({
  toolId: 0,
  version: '',
  changeType: '',
  content: '',
  changer: '',
  changeTime: ''
})

const rules: FormRules = {
  toolId: [{ required: true, message: '请选择工具', trigger: 'change' }],
  content: [{ required: true, message: '请输入变更内容', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getLogPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    if (res.data) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  } catch (error) {
    console.error('加载数据失败', error)
  } finally {
    loading.value = false
  }
}

const loadTools = async () => {
  try {
    const res = await getToolList()
    if (res.data) {
      tools.value = res.data
    }
  } catch (error) {
    console.error('加载工具列表失败', error)
  }
}

const resetForm = () => {
  Object.assign(form, {
    id: undefined,
    toolId: 0,
    version: '',
    changeType: '',
    content: '',
    changer: '',
    changeTime: ''
  })
  formRef.value?.clearValidate()
}

const handleAdd = () => {
  resetForm()
  dialogTitle.value = '新增日志'
  dialogVisible.value = true
}

const handleEdit = (row: ChangeLog) => {
  Object.assign(form, row)
  dialogTitle.value = '编辑日志'
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitting.value = true
    try {
      if (form.id) {
        await updateChangeLog(form.id, form)
        ElMessage.success('更新成功')
      } else {
        await addChangeLog(form)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } catch (error) {
      console.error('提交失败', error)
    } finally {
      submitting.value = false
    }
  })
}

const handleDelete = (row: ChangeLog) => {
  ElMessageBox.confirm('确定要删除这条日志吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteChangeLog(row.id!)
      ElMessage.success('删除成功')
      loadData()
    } catch (error) {
      console.error('删除失败', error)
    }
  })
}

onMounted(() => {
  loadData()
  loadTools()
})
</script>

<style scoped>
.admin-changelogs {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 20px;
  background: var(--card-bg);
  border-radius: 12px;
  border: 2px solid var(--card-border);
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-left h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--color-primary);
}

.add-btn {
  padding: 12px 24px;
  border-radius: 10px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(255,140,0,0.3);
}

.add-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(255,140,0,0.4);
}

.table-card {
  background: var(--card-bg) !important;
  border: 2px solid var(--card-border) !important;
  border-radius: 12px !important;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
