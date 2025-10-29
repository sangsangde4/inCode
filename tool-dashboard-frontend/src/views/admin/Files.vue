<template>
  <div class="admin-files">
    <div class="page-header">
      <div class="header-left">
        <h2>文件管理</h2>
        <el-tag type="info" size="large">总计 {{ total }} 个文件</el-tag>
      </div>
      <el-button type="primary" size="large" @click="dialogVisible = true" class="upload-btn">
        <el-icon><Upload /></el-icon>
        上传文件
      </el-button>
    </div>

    <el-card class="table-card" shadow="never">

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="toolId" label="工具ID" width="100" />
        <el-table-column prop="originalName" label="文件名" min-width="200" />
        <el-table-column prop="version" label="版本" width="100" />
        <el-table-column prop="fileSize" label="文件大小" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="downloadUrlByPath" label="下载路径" min-width="300" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-if="row.downloadUrlByPath" type="success" size="small">
              {{ row.downloadUrlByPath }}
            </el-tag>
            <span v-else class="text-gray">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="downloadCount" label="下载次数" width="100" />
        <el-table-column prop="uploader" label="上传者" width="120" />
        <el-table-column prop="createTime" label="上传时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDownload(row)">下载</el-button>
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

    <!-- 上传对话框 -->
    <el-dialog v-model="dialogVisible" title="上传文件" width="600px">
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
        <el-form-item label="文件说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="选择文件" prop="file">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :file-list="fileList"
          >
            <el-button type="primary">选择文件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="uploading">
          上传
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadFile } from 'element-plus'
import { getFilePage, uploadFile, deleteFile } from '@/api/file'
import { getToolList } from '@/api/tool'
import { useUserStore } from '@/stores/user'
import type { ToolFile, Tool } from '@/types'

const userStore = useUserStore()
const loading = ref(false)
const uploading = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref<ToolFile[]>([])
const tools = ref<Tool[]>([])
const fileList = ref<UploadFile[]>([])

const form = reactive({
  toolId: undefined as number | undefined,
  version: '',
  description: '',
  file: null as File | null
})

const rules: FormRules = {
  toolId: [{ required: true, message: '请选择工具', trigger: 'change' }],
  file: [{ required: true, message: '请选择文件', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getFilePage({
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

const formatFileSize = (size?: number) => {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / 1024 / 1024).toFixed(2) + ' MB'
  return (size / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}

const handleFileChange = (file: UploadFile) => {
  form.file = file.raw || null
  fileList.value = [file]
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    if (!form.file) {
      ElMessage.error('请选择文件')
      return
    }
    
    uploading.value = true
    try {
      const formData = new FormData()
      formData.append('file', form.file)
      formData.append('toolId', String(form.toolId))
      formData.append('version', form.version)
      formData.append('description', form.description)
      formData.append('uploader', userStore.realName)
      
      await uploadFile(formData)
      ElMessage.success('上传成功')
      dialogVisible.value = false
      resetForm()
      loadData()
    } catch (error) {
      console.error('上传失败', error)
    } finally {
      uploading.value = false
    }
  })
}

const resetForm = () => {
  form.toolId = undefined
  form.version = ''
  form.description = ''
  form.file = null
  fileList.value = []
  formRef.value?.clearValidate()
}

const handleDownload = (row: ToolFile) => {
  // 优先使用路径下载URL
  if (row.downloadUrlByPath) {
    window.open(row.downloadUrlByPath, '_blank')
    console.log('使用路径下载:', row.downloadUrlByPath)
  } 
  // 降级使用ID下载URL
  else if (row.downloadUrl) {
    window.open(row.downloadUrl, '_blank')
    console.log('使用ID下载:', row.downloadUrl)
  } 
  // 兜底：使用ID构建下载URL
  else if (row.id) {
    window.open(`/api/files/download/${row.id}`, '_blank')
    console.log('使用ID下载（兜底）:', row.id)
  } else {
    ElMessage.error('无法获取下载地址')
  }
}

const handleDelete = (row: ToolFile) => {
  ElMessageBox.confirm('确定要删除这个文件吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteFile(row.id!)
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
.admin-files {
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

.upload-btn {
  padding: 12px 24px;
  border-radius: 10px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(255,140,0,0.3);
}

.upload-btn:hover {
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

.text-gray {
  color: #909399;
  font-size: 12px;
}
</style>
