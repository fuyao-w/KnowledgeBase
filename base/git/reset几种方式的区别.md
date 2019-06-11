git reset --soft 相当于撤销最后一次 commit，已提交的内容不变。

git reset --mixed 相当于撤销最后一次 add 与 commit，已提交的内容不变。

git reset --hard 相当于撤销最后一次 add 与 commit，同时工作区中的内容恢复到上一次提交的状态。

git reset --keep 在 hard 的基础上，保留未添加到暂存区的文件（未 git add）。

git reset --merge 《branch》 回退到两个分支的父节点。