var data = {
    todoItems: [],
    newItem: { },
    newItemBase: { "id": -1, "parentId": -1, "title": "", "description": "", "done": false, "scheduled": false, "dueDate": "", "repeating": "No", "dueDate": "", "dueTime": "", "dueTimezone": ""  },
    editItem: {  },
    newItemWizardPage: 1,
    editItemWizardPage: 1,
    user: "user",
    version: "n/a",
    showAbout: false,
    showNewItemWizard: false,
    showEditItemWizard: false
}

Vue.component('todoitem', {
    props: ['item'],
    computed: {
        dueDateString: function() {
            var result = this.item.dueDate
            if(this.item.dueTime != null) {
                result += ' '+this.item.dueTime
            }
            return result
        },
        isRepeating: function() {
            return this.item.repeating != 'No'
        }
    },
    template:
    '<li class="pf-c-data-list__item">'+
      '<div class="pf-c-data-list__item-row">'+
        '<div class="pf-c-data-list__item-content">'+
          '<div class="pf-c-data-list__cell item-title">'+
            '{{ item.title }}'+
          '</div>'+
          '<div v-if="item.description.length > 0" class="pf-c-data-list__cell pf-c-data-list__item-action pf-m-hidden pf-m-visible-on-lg">'+
            '{{ item.description }}'+
          '</div>'+
          '<div v-if="isRepeating" class="pf-c-data-list__cell pf-m-hidden pf-c-data-list__item-action pf-m-visible-on-lg repeat-type">'+
            '{{ item.repeating }}'+
          '</div>'+
          '<div v-if="item.scheduled" class="pf-c-data-list__cell due-date">'+
            'Due: {{ dueDateString }}'+
          '</div>'+
        '</div>'+
        '<div class="pf-c-data-list__item-action">'+
          '<button v-if="!item.done" class="pf-c-button pf-m-primary" type="button">'+
            '<i class="fas fa-check-circle"></i>'+
          '</button>'+
          '<button v-else class="pf-c-button pf-m-primary" type="button">'+
            '<i class="fas fa-play"></i>'+
          '</button>'+
          '<button class="pf-c-button pf-m-secondary" type="button">'+
            '<i class="fas fa-edit"></i>'+
          '</button>'+
          '<button v-if="item.repeating != \'No\' || item.done" class="pf-c-button pf-m-danger" type="button">'+
            '<i class="fas fa-minus-circle"></i>'+
          '</button>'+
        '</div>'+
      '</div>'+
    '</li>'
})

var app = new Vue({
    el: '#content',
    data: data,
    computed: {
        activeItems: function() {
            return data.todoItems.filter(function(todoItem) {
                return todoItem.done == false
            })
        },
        inactiveItems: function() {
            return data.todoItems.filter(function(todoItem) {
                return todoItem.done == true
            })
        },
        isFirstPage: function() {
            return data.newItemWizardPage == 1
        }
    },
    methods: {
        activeItemClick: function(event) {
            //console.log("active-todo-item");
            processActiveItemClick(event.target)
        },
        inactiveItemClick: function(event) {
            //console.log("inactive-todo-item");
            processInactiveItemClick(event.target)
        },
        userMenuClick: function(event) {
            //console.log("Menu click!")
            if($(event.target.parentElement).hasClass('pf-m-expanded')) {
                //console.log("Menu close!")
                event.target.setAttribute('aria-expanded', false)
                event.target.parentElement.classList.remove('pf-m-expanded')
                event.target.parentElement.children[1].hidden = true
            } else {
                //console.log("Menu open!")
                event.target.setAttribute('aria-expanded', true)
                event.target.parentElement.classList.add('pf-m-expanded')
                event.target.parentElement.children[1].hidden = false
            }
        },
        logout: function(event) {
            $.post('./api/v1/logout', function(result) {
                data.user = ""
                location.reload()
            }).fail(function(jqXHR, textStatus, errorThrown) {
                console.error('Failed to logout: '+textStatus);
            });
        },
        closeWizard: function(event) {
            //console.log("Close wizard!")
            $('#new-todo-wizard').hide()
            data.newItem = { "id": -1, "parentId": -1, "title": "", "description": "", "done": false, "scheduled": false, "dueDate": "", "repeating": "No", "dueDate": "", "dueTime": "", "dueTimezone": "" }
            data.newItemWizardPage = 1
            data.showEditItemWizard = false
            data.editItemWizardPage = 1
            getItems()
        },
        editWizardBack: function(event) {
            //console.log("Edit wizard back!")
            if(data.editItemWizardPage >= 1) {
                data.editItemWizardPage--
            }
        },
        startNewItemWizard: function(event) {
            console.debug('New item!')
            data.newItem = { "id": -1, "parentId": -1, "title": "", "description": "", "done": false, "scheduled": false, "dueDate": "", "repeating": "No", "dueDate": "", "dueTime": "", "dueTimezone": "" }
            data.showNewItemWizard = true
        },
        editWizardSave: function(event) {
            //console.debug("Edit wizard save!")
            var proceed = true
            if(data.editItemWizardPage === 1) {
                proceed = validateWizardPageOne('edit', data.editItem)
            } else if(data.editItemWizardPage == 2) {
                if(!data.editItem.scheduled) {
                    cleanSchedulingInfo(data.editItem)
                } else {
                    proceed = validateWizardPageTwo('edit', data.editItem)
                }
            }
            if(!proceed) return
            saveTodo(data.editItem)
            //console.debug('Hide wizard')
            data.showEditItemWizard = false
        },
        editWizardNext: function(event) {
            //console.log("Wizard next!")
            var proceed = true
            if(data.editItemWizardPage === 1) {
                proceed = validateWizardPageOne('edit', data.editItem)
            } else if(data.editItemWizardPage == 2) {
                if(!data.editItem.scheduled) {
                    cleanSchedulingInfo(data.editItem)
                } else {
                    proceed = validateWizardPageTwo('edit', data.editItem)
                }
            }
            if(!proceed) return
            data.editItemWizardPage++
        },
        wizardCancel: function(event) {
            //console.log("Cancel wizard!")
            $('#new-todo-wizard').hide()
            data.newItem = data.newItemBase
            data.newItemWizardPage = 1
            data.editItemWizardPage = 1
            data.showEditItemWizard = false
        },
        newWizardBack: function(event) {
            //console.log("Wizard back!")
            if(data.newItemWizardPage >= 1) {
                data.newItemWizardPage--
            }
        },
        newWizardNext: function(event) {
            //console.log("Wizard next!")
            if(data.newItemWizardPage == 1) { // validate input!
                var proceed = validateWizardPageOne('new', data.newItem)
                if(!proceed) return
            } else if(data.newItemWizardPage == 2 && data.newItem.scheduled) { // validate input!
                var proceed = validateWizardPageTwo('new', data.newItem)
                if(!proceed) return
            }
            data.newItemWizardPage++
        },
        newWizardSave: function(event) {
            //console.log("Wizard save!")
            saveTodo(data.newItem)
            data.newItemWizardPage = 1
            data.showNewItemWizard = false
        },
        clickOnAlert: function(event) {
            if(event.target.tagName === "A") {
                var id = event.target.parentElement.parentElement.parentElement.id
                //console.log("Alert id: "+id)
                //console.log("Task id: "+id.substring(6))
                completeItem(id.substring(6))
                getItems()
            }
        },
        toggleAbout: function(event) {
            console.debug('Toggle about!')
            data.showAbout = !data.showAbout
        },
        toggleEditItemWizard: function(event) {
            console.debug('Toggle edit item wizard!')
            data.showEditItemWizard = !data.showEditItemWizard
        },
        toggleNewItemWizard: function(event) {
            console.debug('Toggle new item wizard!')
            data.showNewItemWizard = !data.showNewItemWizard
        }
    }
})

function processActiveItemClick(element) {
    //console.log(element)
    var id = element.parentElement.parentElement.parentElement.id
    if($(element).hasClass("pf-m-primary")) {
        //console.log(id+" primary!")
        completeItem(id)
        getItems()
    } else if($(element).hasClass("pf-m-secondary")) {
        //console.log(id+" secondary!")
        editItem(id)
    } else if($(element).hasClass("pf-m-tertiary")) {
        //console.log(id+" tertiary!")
        editItem(id)
        data.editItemWizardPage=2
    } else if($(element).hasClass('pf-m-danger')) {
        //console.log(id+" danger!")
        deactivateItem(id)
    }
}

function editItem(itemId) {
    for(var i=0; i < data.todoItems.length; i++) {
        if(data.todoItems[i].id == itemId) {
            //console.log("id: "+data.editItem.id)
            data.editItem = data.todoItems[i]
            data.showEditItemWizard = true
            break;
        }
    }
}

function activateItem(id) {
    //console.log('/api/v1/items/'+id+'/activate')
    $.getJSON('./api/v1/items/'+id+'/activate', function(result) {
        for(var i=0; i < data.todoItems.length; ++i) {
            if(data.todoItems[i].id == id) {
                publishSuccessAlert(id, data.todoItems[i].title+' activated!')
                break;
            }
        }
        getItems()
    }).fail(function(jqXHR, textStatus, errorThrown) {
        for(var i=0; i < data.todoItems.length; ++i) {
            if(data.todoItems[i].id == id) {
                publishDangerAlert(id, 'Failed to activate '+data.todoItems[i].title)
                break;
            }
        }
        console.error('Failed set event '+id+' done: '+textStatus);
        getItems()
    });
}

function deactivateItem(id) {
    //console.log('/api/v1/items/'+id+'/deactivate')
    $.getJSON('./api/v1/items/'+id+'/deactivate', function(result) {
        for(var i=0; i < data.todoItems.length; ++i) {
            if(data.todoItems[i].id == id) {
                publishSuccessAlert(id, data.todoItems[i].title+' activated!')
                break;
            }
        }
        getItems()
    }).fail(function(jqXHR, textStatus, errorThrown) {
        for(var i=0; i < data.todoItems.length; ++i) {
            if(data.todoItems[i].id == id) {
                publishDangerAlert(id, 'Failed to activate '+data.todoItems[i].title)
                break;
            }
        }
        console.error('Failed set event '+id+' done: '+textStatus);
        getItems()
    });
    for(var i=0; i < data.todoItems.length; ++i) {
        if(data.todoItems[i].id == id) {
            publishSuccessAlert(id, data.todoItems[i].title+' deactivated!')
            publishDangerAlert(id, 'Failed to deactivate '+data.todoItems[i].title)
            break;
        }
    }
}

function processInactiveItemClick(element) {
    //console.log(element)
    var id = element.parentElement.parentElement.parentElement.id
    if($(element).hasClass("pf-m-primary")) {
        //console.log(id+" primary!")
        activateItem(id)
    } else if($(element).hasClass("pf-m-secondary")) {
        //console.log(id+" secondary!")
        editItem(id)
    } else if($(element).hasClass("pf-m-tertiary")) {
        //console.log(id+" tertiary!")
    } else if($(element).hasClass("pf-m-danger")) {
        //console.log(id+" danger!")
        //console.log('/api/v1/items/'+id+'/delete')
        $.getJSON('./api/v1/items/'+id+'/delete', function(result) {
            for(var i=0; i < data.todoItems.length; ++i) {
                if(data.todoItems[i].id == id) {
                    publishSuccessAlert(id, data.todoItems[i].title+' deleted!')
                    break;
                }
            }
            getItems()
        }).fail(function(jqXHR, textStatus, errorThrown) {
            for(var i=0; i < data.todoItems.length; ++i) {
                if(data.todoItems[i].id == id) {
                    publishDangerAlert(id, 'Failed to delete '+data.todoItems[i].title)
                    break;
                }
            }
            console.error('Failed set event '+id+' done: '+textStatus);
            getItems()
        });
    }
}

function publishAlert(html, itemId, delay) {
    if($('#'+itemId).length) {
        $('#'+itemId).remove()
    }
    $('#alerts').append(html)
    if(delay > 0) {
        setTimeout(function(id) {
            //console.log("Fade "+id)
            $('#'+id).fadeOut(1500)
            //$('#alert'+id).remove()
        }, delay, itemId)
    }
}

function completeItem(id) {
    //console.log('/api/v1/items/'+id+'/done')
    $.getJSON('./api/v1/items/'+id+'/done', function(result) {
        for(var i=0; i < data.todoItems.length; ++i) {
            if(data.todoItems[i].id == id) {
                publishSuccessAlert(id, data.todoItems[i].title+' completed!')
                break;
            }
        }
        getItems()
    }).fail(function(jqXHR, textStatus, errorThrown) {
        console.error('Failed set event '+id+' done: '+textStatus);
        for(var i=0; i < data.todoItems.length; ++i) {
            if(data.todoItems[i].id == id) {
                publishDangerAlert(id, 'Failed to set '+data.todoItems[i].title+' completed!')
                break;
            }
        }
        getItems()
    });
}

function getItems() {
    //console.log("./api/v1/items")
    $.getJSON("./api/v1/items", function(result) {
        data.todoItems = result;
    }).fail(function(jqXHR, textStatus, errorThrown) {
        //console.log("Status: "+jqXHR.status)
        console.error("Failed to get items: "+textStatus)
        if(jqXHR.status == 404) {
            //console.log("./api/v/new")
            $.getJSON("./api/v1/new", function(result) {
                publishSuccessAlert('alertsn', 'Added new list!')
            }).fail(function(jqXHR, textStatus, errorThrown) {
                console.error("Failed to add new list: "+textStatus)
                publishDangerAlert('alertdn', 'Failed to add new list!', 5000)
            })
        } else {
            publishDangerAlert('alertdi', 'Failed to get items!', 5000)
        }
    })
}
getItems()

// New item wizard shit
function validateWizardPageOne(prefix, item) {
    if(item.title === "") {
        $('#'+prefix+'-item-title').attr('aria-invalid', "true")
        if($('#'+prefix+'-item-title-helper').length == 0) {
            var helperText = '<p class="pf-c-form__helper-text pf-m-error" id="'+prefix+'-item-title-helper" aria-live="polite">You need to specify title for todo items.</p>'
            $(helperText).insertAfter('#'+prefix+'-item-title')
        }
        return false
    } else {
        $('#'+prefix+'-item-title').attr('aria-invalid', "false")
        $('#'+prefix+'-item-title-helper').remove()
    }
    return true
}

function validateWizardPageTwo(prefix, item) {
    if(item.dueDate.match(/[0-9]{4}-[0-1][0-9]-[0-3][0-9]/) == null) {
        $('#'+prefix+'-item-duedate-date').attr('aria-invalid', 'true')
        var helperText = '<p class="pf-c-form__helper-text pf-m-error" id="'+prefix+'-item-duedate-date-helper" aria-live="polite">When the todo item is scheduled you need to specify due date in format yyyy-mm-dd.</p>'
        if($('#'+prefix+'-item-duedate-date-helper').length == 0) {
            $(helperText).insertAfter('#'+prefix+'-item-duedate-date')
        }
        return false
    } else {
        $('#'+prefix+'-item-duedate-date').attr('aria-invalid', "false")
        $('#'+prefix+'-item-duedate-date-helper').remove()
    }
    if(item.dueTime != null && item.dueTime != "" && item.dueTime.match(/[0-2][0-9]:[0-5][0-9]/) == null) {
        $('#'+prefix+'-item-duedate-time').attr('aria-invalid', 'true')
        var helperText = '<p class="pf-c-form__helper-text pf-m-error" id="'+prefix+'-item-duedate-time-helper" aria-live="polite">If you specify due time, it needs to be specified in format HH:MM</p>'
        if($('#'+prefix+'-item-duedate-time-helper').length == 0) {
            $(helperText).insertAfter('#'+prefix+'-item-duedate-time')
        }
        return false
    } else {
        $('#'+prefix+'-item-duedate-time').attr('aria-invalid', "false")
        $('#'+prefix+'-item-duedate-time-helper').remove()
    }
    if(item.dueTimezone != null && item.dueTimezone != "" && item.dueTimezone.match(/[+-]?[0-1][0-9]:[0-5][0-9]/) == null) {
        $('#'+prefix+'-item-duedate-tx').attr('aria-invalid', 'true')
        var helperText = '<p class="pf-c-form__helper-text pf-m-error" id="'+prefix+'-item-duedate-tz-helper" aria-live="polite">If you specify time zone, it needs to be specified in format [+-]?HH:MM</p>'
        if($('#'+prefix+'-item-duedate-tz-helper').length == 0) {
            $(helperText).insertAfter('#'+prefix+'-item-duedate-tz')
        }
        return false
    } else {
        $('#'+prefix+'-item-duedate-tz').attr('aria-invalid', "false")
        $('#'+prefix+'-item-duedate-tz-helper').remove()
    }
    return true
}

function cleanSchedulingInfo(item) {
    item.dueDate = ""
    item.dueTime = ""
    item.dueTimezone = ""
    item.repeating = "No"
}

function saveTodo(item) {
    if(item.id < 0) {
        //console.log("./api/v1/items/add")
        $.ajax({
            url: './api/v1/items/add',
            type: 'post',
            dataType: 'json',
            contentType: 'application/json',
            success: function(result) {
                publishSuccessAlert(item.id, item.title+' added!')
                getItems()
            },
            fail: function(jqXHR, textStatus, errorThrown) {
                publishDangerAlert(item.id, 'Failed to add '+item.title+'!')
                console.error("Failed to add "+item.title+": "+textStatus);
            },
            data: JSON.stringify(item)
        });
    } else {
        //console.log("./api/v1/items/"+item.id+"/update")
        $.ajax({
            url: './api/v1/items/'+item.id+'/update',
            type: 'post',
            dataType: 'json',
            contentType: 'application/json',
            success: function(result) {
                publishSuccessAlert(item.id, item.title+' saved!')
                getItems()
            },
            fail: function(jqXHR, textStatus, errorThrown) {
                publishDangerAlert(item.id, 'Failed to save '+item.title+'!')
                console.error("Failed to save "+item.title+": "+textStatus);
            },
            data: JSON.stringify(item)
        });
    }
}

function publishSuccessAlert(id, message) {
    //console.log("publishSuccessAlert("+id+", "+message+")")
    var alert = '<li class="pf-c-alert-group__item" id=alerts'+id+' onclick="$(this).remove()">'+
                  '<div class="pf-c-alert pf-m-success" aria-label="Success alert" >'+
                    '<div class="pf-c-alert__icon">'+
                      '<i class="fas fa-check-circle" aria-hidden="true"></i>'+
                    '</div>'+
                    '<h4 class="pf-c-alert__title">'+
                      '<span class="pf-screen-reader">Success alert:</span>'+message+
                    '</h4>'+
                    '<div class="pf-c-alert__action" onclick="$(this).parent().parent().remove()">'+
                      '<button class="pf-c-button pf-m-plain" type="button" aria-label="Close success alert: '+message+'">'+
                        '<i class="fas fa-times" aria-hidden="true"></i>'+
                      '</button>'+
                    '</div>'+
                  '</div>'+
                '</li>'
    publishAlert(alert, 'alerts'+id, 5000)
}

function publishDangerAlert(id, message, timeout=0) {
    //console.log("publishSaveDangerAlert("+id+", "+message+")")
    var alert = '<li class="pf-c-alert-group__item" id=alertd'+id+' onclick="$(this).remove()">'+
                  '<div class="pf-c-alert pf-m-danger" aria-label="Danger alert">'+
                    '<div class="pf-c-alert__icon">'+
                      '<i class="fas fa-exclamation-circle" aria-hidden="true"></i>'+
                    '</div>'+
                    '<h4 class="pf-c-alert__title">'+
                      '<span class="pf-screen-reader">Danger alert:</span>'+message+
                    '</h4>'+
                    '<div class="pf-c-alert__action" onclick="$(this).parent().parent().remove()">'+
                      '<button class="pf-c-button pf-m-plain" type="button" aria-label="Close warning alert: '+message+'">'+
                        '<i class="fas fa-times" aria-hidden="true"></i>'+
                      '</button>'+
                    '</div>'+
                  '</div>'+
                '</li>'
    publishAlert(alert, 'alertd'+id, timeout)
}

function getUser() {
    $.getJSON("./api/v1/user", function(result) {
        data.user = result.user;
    }, "json").fail(function(jqXHR, textStatus, errorThrown) {
        publishDangerAlert('alertdu', 'Failed to get user!', 5000)
        console.error("Failed to get user: "+textStatus);
    });
}
getUser()

function getVersion() {
    $.getJSON("./api/v1/version", function(result) {
        data.version = result.version;
    }, "json").fail(function(jqXHR, textStatus, errorThrown) {
        publishDangerAlert('alertdv', 'Failed to get version!', 5000)
        console.error("Failed to get version: "+textStatus);
    });
}
getVersion()

function checkOverdueItems() {
    var now = new Date()
    var nowDate = now.toISOString().split('T')[0]
    var tomorrow = new Date()
    tomorrow.setDate(tomorrow.getDate()+1)
    var tomorrowDate = tomorrow.toISOString().split('T')[0]
    for(var i=0; i < data.todoItems.length; ++i) {
        console.log("Item: "+data.todoItems[i].id)
        if(!data.todoItems[i].done && data.todoItems[i].scheduled) {
            var due = new Date(data.todoItems[i].dueDate)
            var dueDate = due.toISOString().split('T')[0]
            console.log("Now: "+nowDate+" Due: "+dueDate)
            if(nowDate === dueDate) {
                console.log(data.todoItems[i].id+' due today!')

                var alert =  '<li class="pf-c-alert-group__item" id=alerti'+data.todoItems[i].id+'>'+
                        '<div class="pf-c-alert pf-m-info" aria-label="Information alert">'+
                        '<div class="pf-c-alert__icon">'+
                        '<i class="fas fa-info-circle" aria-hidden="true"></i>'+
                        '</div>'+
                        '<h4 class="pf-c-alert__title">'+
                        '<span class="pf-screen-reader">Information alert:</span>'+data.todoItems[i].title+' is due today!'+
                        '</h4>'+
                        '<div class="pf-c-alert__description">'+
                        '<a href="#" onclick="$(this).parent().parent().parent().remove()">Set todo as done</a>'+
                        '</div>'+
                        '<div class="pf-c-alert__action" onclick="$(this).parent().parent().remove()">'+
                        '<button class="pf-c-button pf-m-plain" type="button" aria-label="Close information alert: '+data.todoItems[i].title+'">'+
                        '<i class="fas fa-times" aria-hidden="true"></i>'+
                        '</button>'+
                        '</div>'+
                        '</div>'+
                        '</li>'

                publishAlert(alert, 'alerti'+data.todoItems[i].id, 0)
            } else if(due < now && data.todoItems[i].repeating !== "No") {
                console.log(data.todoItems[i].id+' overdue!')

                var alert = '<li class="pf-c-alert-group__item" id=alertw'+data.todoItems[i].id+'>'+
                        '<div class="pf-c-alert pf-m-warning" aria-label="Warning alert">'+
                        '<div class="pf-c-alert__icon">'+
                        '<i class="fas fa-exclamation-triangle" aria-hidden="true"></i>'+
                        '</div>'+
                        '<h4 class="pf-c-alert__title">'+
                        '<span class="pf-screen-reader">Warning alert:</span>'+data.todoItems[i].title+' is overdue!'+
                        '</h4>'+
                        '<div class="pf-c-alert__description">'+
                        '<a href="#" onclick="$(this).parent().parent().parent().remove()">Set todo as done</a>'+
                        '</div>'+
                        '<div class="pf-c-alert__action" onclick="$(this).parent().parent().remove()">'+
                        '<button class="pf-c-button pf-m-plain" type="button" aria-label="Close warning alert: '+data.todoItems[i].title+'">'+
                        '<i class="fas fa-times" aria-hidden="true"></i>'+
                        '</button>'+
                        '</div>'+
                        '</div>'+
                        '</li>'

                publishAlert(alert, 'alertw'+data.todoItems[i].id, 0)
            } else if(tomorrowDate == dueDate){
                console.log("Due tomorrow!");

                var alert =  '<li class="pf-c-alert-group__item" id=alerti'+data.todoItems[i].id+'>'+
                        '<div class="pf-c-alert pf-m-info" aria-label="Information alert">'+
                        '<div class="pf-c-alert__icon">'+
                        '<i class="fas fa-info-circle" aria-hidden="true"></i>'+
                        '</div>'+
                        '<h4 class="pf-c-alert__title">'+
                        '<span class="pf-screen-reader">Information alert:</span>'+data.todoItems[i].title+' is due tomorrow!'+
                        '</h4>'+
                        '<div class="pf-c-alert__description">'+
                        '<a href="#" onclick="$(this).parent().parent().parent().remove()">Set todo as done</a>'+
                        '</div>'+
                        '<div class="pf-c-alert__action" onclick="$(this).parent().parent().remove()">'+
                        '<button class="pf-c-button pf-m-plain" type="button" aria-label="Close information alert: '+data.todoItems[i].title+'">'+
                        '<i class="fas fa-times" aria-hidden="true"></i>'+
                        '</button>'+
                        '</div>'+
                        '</div>'+
                        '</li>'

                publishAlert(alert, 'alerti'+data.todoItems[i].id, 5000)
            }
        }
    }
}
setTimeout(checkOverdueItems, 1500)
