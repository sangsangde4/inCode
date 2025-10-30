<template>
  <div class="admin-files">
    <div class="page-header">
      <div class="header-left">
        <h2>文件管理</h2>
        <el-tag type="info" size="large">总计 {{ tableData.length }} 个文件</el-tag>
      </div>
      <el-button type="primary" size="large" @click="dialogVisible = true" class="upload-btn">
        <el-icon><Upload /></el-icon>
        上传文件
      </el-button>
    </div>

    <el-card class="table-card" shadow="never">
      <FileBrowser 
        :files="tableData" 
        :loading="loading"
        @download="handleDownload"
        @delete="handleDelete"
      />
    </el-card>

    <!-- 上传对话框 -->
    <el-dialog v-model="dialogVisible" title="上传文件" width="600px" @close="handleDialogClose">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="margin-top: 20px;">
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
          <el-input v-model="form.version" placeholder="如：1.0.0（必填）" />
        </el-form-item>
        <el-form-item label="架构类型" prop="architecture">
          <el-select v-model="form.architecture" style="width: 100%" clearable placeholder="选择架构（可选）">
            <el-option label="Linux x64" value="linux_x64" />
            <el-option label="Linux ARM64" value="linux_arm64" />
            <el-option label="Windows x64" value="windows_x64" />
            <el-option label="Windows x86" value="windows_x86" />
            <el-option label="Windows ARM64" value="windows_arm64" />
            <el-option label="macOS x64 (Intel)" value="macos_x64" />
            <el-option label="macOS ARM64 (Apple Silicon)" value="macos_arm64" />
            <el-option label="通用 / 其他" value="universal" />
          </el-select>
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
            <template #tip>
              <div class="el-upload__tip">
                支持最大2GB文件上传
              </div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item v-if="uploading && uploadProgress > 0">
          <el-progress 
            :percentage="uploadProgress" 
            :status="uploadProgress === 100 ? 'success' : undefined"
            :stroke-width="20"
          >
            <template #default="{ percentage }">
              <span class="percentage-value">{{ percentage }}%</span>
            </template>
          </el-progress>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false" :disabled="uploading">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="uploading">
          {{ uploading ? '上传中...' : '上传' }}
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
import { createVersionValidationRule } from '@/utils/semanticVersion'
import FileBrowser from '@/components/FileBrowser.vue'
import type { ToolFile, Tool } from '@/types'

const userStore = useUserStore()
const loading = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const tableData = ref<ToolFile[]>([])
const tools = ref<Tool[]>([])
const fileList = ref<UploadFile[]>([])

const form = reactive({
  toolId: undefined as number | undefined,
  version: '',
  architecture: '',
  description: '',
  file: null as File | null
})

const rules: FormRules = {
  toolId: [{ required: true, message: '请选择工具', trigger: 'change' }],
  version: [createVersionValidationRule(true)], // 版本号必填
  file: [{ required: true, message: '请选择文件', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    // 获取所有文件（不分页）
    const res = await getFilePage({
      pageNum: 1,
      pageSize: 10000 // 获取所有文件
    })
    if (res.data) {
      tableData.value = res.data.records
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
      if (form.architecture) {
        formData.append('architecture', form.architecture)
      }
      formData.append('description', form.description)
      formData.append('uploader', userStore.realName)
      
      // 上传文件并显示进度
      await uploadFile(formData, (progressEvent: any) => {
        if (progressEvent.total) {
          uploadProgress.value = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        }
      })
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
  form.architecture = ''
  form.description = ''
  form.file = null
  fileList.value = []
  uploadProgress.value = 0
  formRef.value?.clearValidate()
}

const handleDialogClose = () => {
  resetForm()
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
  box-shadow: 0 4px 12px rgba(0, 217, 255, 0.25);
}

.upload-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 217, 255, 0.35);
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

.el-upload__tip {
  color: var(--text-secondary);
  font-size: 12px;
  margin-top: 8px;
}

.percentage-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-primary);
}

/* 进度条样式优化 */
.el-progress {
  margin-top: 10px;
}

[data-theme='dark'] .el-progress__text {
  color: var(--text-primary) !important;
}
</style>
